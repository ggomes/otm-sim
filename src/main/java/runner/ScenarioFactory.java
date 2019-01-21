/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package runner;

import actuator.*;
import actuator.sigint.ActuatorSignal;
import commodity.*;
import commodity.Commodity;
import commodity.Subnetwork;
import common.Link;
import common.Network;
import common.Node;
import control.*;
import control.sigint.ControllerSignalPretimed;
import error.OTMErrorLog;
import error.OTMException;
import common.*;
import jaxb.*;
import keys.DemandType;
import keys.KeyCommodityDemandTypeId;
import keys.KeyCommodityLink;
import packet.PacketSplitter;
import plugin.PluginLoader;
import profiles.*;
import sensor.AbstractSensor;
import sensor.FixedSensor;
import utils.OTMUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toSet;

public class ScenarioFactory {

    ///////////////////////////////////////////
    // public
    ///////////////////////////////////////////

    public static runner.Scenario create_scenario(jaxb.Scenario js, boolean validate) throws OTMException {

        OTMUtils.reset_counters();

        Scenario scenario = new Scenario();

        // plugins ..........................................................
        PluginLoader.load_plugins( js.getPlugins() );

        // network ...........................................................
        scenario.network = ScenarioFactory.create_network_from_jaxb(scenario, js.getModels(), js.getNetwork());

        // control ...............................................
        scenario.actuators = ScenarioFactory.create_actuators_from_jaxb(scenario, js.getActuators() );
        scenario.sensors = ScenarioFactory.create_sensors_from_jaxb(scenario, js.getSensors() );
        scenario.controllers = ScenarioFactory.create_controllers_from_jaxb(scenario,js.getControllers() );

        // commodities ......................................................
        scenario.subnetworks = ScenarioFactory.create_subnetworks_from_jaxb(
                scenario.network,
                js.getSubnetworks() ,
                have_global_commodity(js.getCommodities()) );

        scenario.commodities = ScenarioFactory.create_commodities_from_jaxb(
                scenario,
                scenario.subnetworks,
                js.getCommodities());

        // tell links about commodities ...................................
        for(Commodity c : scenario.commodities.values())
            for (Subnetwork subnet : c.subnetworks )
                for (Link link : subnet.links)
                    link.add_commodity(c);

        // tell nodes about commodities ....................................
        for (Node node : scenario.network.nodes.values())
            node.set_commodities();

        // populate link.path2outlink and lanegroup.path2roadconnections
        Set<Subnetwork> used_paths = scenario.commodities.values().stream()
                .filter(c->c.pathfull)
                .map(c->c.subnetworks)
                .flatMap(c->c.stream())
                .collect(toSet());

        for(Subnetwork subnet : used_paths){
            if(!subnet.is_path)
                continue; // this should not happen. They should all be paths
            Path path = (Path) subnet;
            for(Link link : path.links){
                Link next_link = path.get_link_following(link);
                link.path2outlink.put(path.getId(),next_link==null?null:next_link.getId());
            }
        }

        // branders ........................................................
        // build branders for non-sink non-one2one links
        scenario.network.links.values().stream()
                .filter(link -> !link.is_sink && !link.end_node.is_many2one)
                .forEach(link -> link.packet_splitter = new PacketSplitter(link));

        // splits ...........................................................
        ScenarioFactory.create_splits_from_jaxb(scenario.network, js.getSplits());

        // demands ..........................................................
        scenario.data_demands = ScenarioFactory.create_demands_from_jaxb(scenario.network, js.getDemands());

        // tell the network about macro sources
//        scenario.network.macro_sources = new HashSet<>();
//        scenario.network.macro_sources.addAll( scenario.data_demands.values().stream()
//                .map(d->d.source)
//                .filter(s->s instanceof models.ctm.VehicleSource)
//                .map (x -> (models.ctm.VehicleSource) x)
//                .collect(Collectors.toSet()) );

//        // register vehicle events requests with commodities
//        for(AbstractOutput or : scenario.outputs){
//            if(or.type== AbstractOutput.Type.vehicle){
//                EventsVehicle ev = (EventsVehicle)or;
//                if(ev.commodity_id!=null) {
//                    Commodity commodity = scenario.commodities.get(ev.commodity_id);
//                    if(commodity==null)
//                        throw new OTMException("Bad commodity id");
//                    commodity.set_vehicle_event_listener(ev);
//                } else
//                    for(Commodity c : scenario.commodities.values())
//                        c.set_vehicle_event_listener(ev);
//            }
//        }

        // validate ................................................
        if(validate) {
            OTMErrorLog errorLog = scenario.validate();
            errorLog.check();
        }

        return scenario;
    }

    public static runner.Scenario create_scenario_for_static_traffic_assignment(jaxb.Scenario js) throws OTMException {

        OTMUtils.reset_counters();

        Scenario scenario = new Scenario();

        // common ...........................................................
        scenario.network = new common.Network(
                scenario ,
                js.getNetwork().getNodes().getNode(),
                js.getNetwork().getLinks().getLink(),
                js.getNetwork().getRoadparams() );

        // commodities
        scenario.commodities = new HashMap<>();
        for (jaxb.Commodity jaxb_comm : js.getCommodities().getCommodity())
            scenario.commodities.put( jaxb_comm.getId(),  new Commodity( jaxb_comm,null,scenario) );

        // OD node map
        Map<Long,Long> origin_nodes = new HashMap<>();
        Map<Long,Long> destination_nodes = new HashMap<>();
        for(jaxb.Subnetwork subnetwork : js.getSubnetworks().getSubnetwork() ){
            List<Long> link_ids = OTMUtils.csv2longlist(subnetwork.getContent());
            Link origin_link = scenario.network.links.get(link_ids.get(0));
            origin_nodes.put(subnetwork.getId(), origin_link.start_node.getId());
            Link destination_link = scenario.network.links.get(link_ids.get(link_ids.size()-1));
            destination_nodes.put(subnetwork.getId(),destination_link.end_node.getId());
        }

        // demands ..........................................................
        scenario.data_demands = new HashMap<>();
        if (js.getDemands()!=null)
            for (jaxb.Demand jaxb_demand : js.getDemands().getDemand()) {

                Commodity comm = scenario.commodities.get(jaxb_demand.getCommodityId());
                Long subnetwork_id = jaxb_demand.getSubnetwork();
                Long origin_node = origin_nodes.get(subnetwork_id);
                Long destination_node = destination_nodes.get(subnetwork_id);

                DemandProfileOD dp = new DemandProfileOD(jaxb_demand,comm,origin_node,destination_node);
                KeyCommodityDemandTypeId key = new KeyCommodityDemandTypeId(comm.getId(),subnetwork_id,DemandType.pathfull);
                scenario.data_demands.put(key,dp);
            }

        return scenario;
    }

    ///////////////////////////////////////////
    // private static
    ///////////////////////////////////////////

//    private static void set_global_model(jaxb.Scenario js,String global_model) throws OTMException {
//        if(global_model==null)
//            return;
//
//        Model model = new Model();
//
//        // all link ids
//        List<Long> link_ids = js.getNetwork().getLinks().getLink().stream().map(link->link.getId()).collect(Collectors.toList());
//        String all_links = OTMUtils.comma_format(link_ids);
//
//        switch(global_model){
//            case "pq":
//                PointQueue pq = new PointQueue();
//                pq.setContent(all_links);
//                model.setPointQueue(pq);
//                break;
//            case "ctm":
//                Ctm ctm = new Ctm();
//                ctm.setContent(all_links);
//                ctm.setMaxCellLength(100f);     // default to 100 meters
//                model.setCtm(ctm);
//                break;
//            case "mn":
//                Mn mn = new Mn();
//                mn.setContent(all_links);
//                mn.setMaxCellLength(100f);     // default to 100 meters
//                model.setMn(mn);
//                break;
//            default:
//                throw new OTMException("Bad global model: " + global_model);
//        }
//
////        System.out.println("Warning: Overwriting the link model. Setting all to " + global_model);
//
//        js.setModel(model);
//
//    }

    private static common.Network create_network_from_jaxb(Scenario scenario, jaxb.Models jaxb_models,jaxb.Network jaxb_network) throws OTMException {
        common.Network network = new common.Network(
                scenario ,
                jaxb_models==null ? null : jaxb_models.getModel(),
                jaxb_network.getNodes().getNode(),
                jaxb_network.getLinks().getLink(),
                jaxb_network.getRoadgeoms() ,
                jaxb_network.getRoadconnections() ,
                jaxb_network.getRoadparams() );
        return network;
    }

    private static Map<Long, AbstractSensor> create_sensors_from_jaxb(Scenario scenario, jaxb.Sensors jaxb_sensors) throws OTMException {
        HashMap<Long, AbstractSensor> sensors = new HashMap<>();
        if(jaxb_sensors==null)
            return sensors;
        for(jaxb.Sensor jaxb_sensor : jaxb_sensors.getSensor()){
            AbstractSensor sensor;
            switch(jaxb_sensor.getType()){
                case "fixed":
                    sensor = new FixedSensor(scenario,jaxb_sensor);
                    break;
                default:
                    sensor = null;
                    break;
            }
            sensors.put(jaxb_sensor.getId(),sensor);
        }
        return sensors;
    }

    private static Map<Long, AbstractActuator> create_actuators_from_jaxb(Scenario scenario, jaxb.Actuators jaxb_actuators) throws OTMException {
        HashMap<Long, AbstractActuator> actuators = new HashMap<>();
        if(jaxb_actuators==null)
            return actuators;
        for(jaxb.Actuator jaxb_actuator : jaxb_actuators.getActuator()){
            AbstractActuator actuator;
            switch(jaxb_actuator.getType()){
                case "signal":
                    actuator = new ActuatorSignal(scenario,jaxb_actuator);
                    break;
                case "ramp_meter":
                    actuator = new ActuatorRampMeter(scenario,jaxb_actuator);
                    break;
                case "vsl":
                    actuator = new ActuatorVSL(scenario,jaxb_actuator);
                    break;
                case "fd":
                    actuator = new ActuatorFD(scenario,jaxb_actuator);
                    break;
                default:
                    actuator = null;
                    break;
            }
            actuators.put(jaxb_actuator.getId(),actuator);
        }
        return actuators;
    }

    private static Map<Long, AbstractController> create_controllers_from_jaxb(Scenario scenario, jaxb.Controllers jaxb_controllers) throws OTMException {
        HashMap<Long, AbstractController> controllers = new HashMap<>();
        if(jaxb_controllers==null)
            return controllers;
        for(jaxb.Controller jaxb_controller : jaxb_controllers.getController()){
            AbstractController controller;
            String controller_type = jaxb_controller.getType();
            switch(controller_type){
                case "irm_tod":
                    controller = null;
                    break;
                case "sig_pretimed":
                    controller = new ControllerSignalPretimed(scenario,jaxb_controller);
                    break;
                default:

                    // it might be a plugin
                    controller = PluginLoader.get_controller_instance(controller_type,scenario,jaxb_controller);
                    break;
            }
            controllers.put(jaxb_controller.getId(),controller);
        }
        return controllers;
    }

    private static Map<Long, Subnetwork> create_subnetworks_from_jaxb(Network network, jaxb.Subnetworks jaxb_subnets,boolean have_global_commodity) throws OTMException {

        HashMap<Long, Subnetwork> subnetworks = new HashMap<>();

        if(jaxb_subnets!=null && jaxb_subnets.getSubnetwork().stream().anyMatch(x->x.getId()==0L))
            throw new OTMException("Subnetwork id '0' is not allowed.");

        // create global commodity
        if(have_global_commodity) {
            Subnetwork subnet = new Subnetwork(network);
            subnetworks.put(0l, subnet.is_path ? new Path(network) : subnet );
        }

        if ( jaxb_subnets != null ){
            // initialize
            for (jaxb.Subnetwork jaxb_subnet : jaxb_subnets.getSubnetwork()) {
                if (subnetworks.containsKey(jaxb_subnet.getId()))
                    throw new OTMException("Repeated subnetwork id");
                Subnetwork subnet = new Subnetwork(jaxb_subnet,network);
                subnetworks.put(jaxb_subnet.getId(), subnet.is_path ? new Path(jaxb_subnet,network) : subnet );
            }
        }

//        // build subnetwork of lanegroups
//        for(Subnetwork subnetwork : subnetworks.values()){
//
//            // special case for global subnetworks
//            if(subnetwork.is_global) {
//                subnetwork.add_lanegroups(network.get_lanegroups());
//                continue;
//            }
//
//            for(Link link : subnetwork.links){
//
//                // case single lane group, then add it and continue
//                // this takes care of the one2one case
//                if(link.lanegroups_flwdn.size()==1){
//                    subnetwork.add_lanegroup( link.lanegroups_flwdn.values().iterator().next());
//                    continue;
//                }
//
//                // lane covered by road connections from inputs to here
//                Set<Integer> input_lanes = new HashSet<>();
//                if(link.is_source)
//                    input_lanes.addAll(link.get_up_lanes());
//                else {
//                    Set<Link> inputs = OTMUtils.intersect(link.start_node.in_links.values(), subnetwork.links);
//                    for (Link input : inputs) {
//                        if (input.outlink2lanegroups.containsKey(link.getId())) {
//                            for (AbstractLaneGroup lg : input.outlink2lanegroups.get(link.getId())) {
//                                RoadConnection rc = lg.get_roadconnection_for_outlink(link.getId());
//                                if (rc != null)
//                                    input_lanes.addAll(IntStream.rangeClosed(rc.end_link_from_lane, rc.end_link_to_lane)
//                                            .boxed().collect(toSet()));
//                            }
//                        }
//                    }
//                }
//
//                // lane covered by road connections from here to outputs
//                Set<Integer> output_lanes = new HashSet<>();
//                if(link.is_sink) {
//                    for(int lane=1;lane<=link.get_num_dn_lanes();lane++)
//                        output_lanes.add(lane);
//                }
//                else {
//                    Set<Link> outputs = OTMUtils.intersect(link.end_node.out_links.values(), subnetwork.links);
//                    for (Link output : outputs)
//                        for (AbstractLaneGroup lg : link.lanegroups_flwdn.values()) {
//                            RoadConnection rc = lg.get_roadconnection_for_outlink(output.getId());
//                            if (rc != null)
//                                output_lanes.addAll(IntStream.rangeClosed(rc.start_link_from_lane, rc.start_link_to_lane)
//                                        .boxed().collect(toSet()));
//                        }
//                }
//
//                // add the lanegroups that cover the intersection of the two
//                Set<Integer> subnetlanes = OTMUtils.intersect(input_lanes,output_lanes);
//                for(Integer lane : subnetlanes)
//                    subnetwork.add_lanegroup( link.get_lanegroup_for_dn_lane(lane) ); // TODO FIX THIS!!!
//            }
//
//        }

        return subnetworks;
    }

    private static Map<Long, Commodity> create_commodities_from_jaxb(Scenario scenario,Map<Long, Subnetwork> subnetworks, jaxb.Commodities jaxb_commodities) throws OTMException {

        HashMap<Long, Commodity> commodities = new HashMap<>();

        if (jaxb_commodities == null)
            return commodities;

        for (jaxb.Commodity jaxb_comm : jaxb_commodities.getCommodity()) {
            if (commodities.containsKey(jaxb_comm.getId()))
                throw new OTMException("Repeated commodity id in <commodities>");

            List<Long> subnet_ids = new ArrayList<>();
            boolean is_global = !jaxb_comm.isPathfull() && (jaxb_comm.getSubnetworks()==null || jaxb_comm.getSubnetworks().isEmpty());
            if(is_global)
                subnet_ids.add(0L);
            else
                subnet_ids = OTMUtils.csv2longlist(jaxb_comm.getSubnetworks());

            // check subnetwork ids are good
            if(!is_global && !subnetworks.keySet().containsAll(subnet_ids))
                throw new OTMException(String.format("Bad subnetwork id in commodity %d",jaxb_comm.getId()) );

            Commodity comm = new Commodity( jaxb_comm,subnet_ids,scenario);
            commodities.put( jaxb_comm.getId(), comm );

            // inform the subnetwork of their commodities
            if(!is_global)
                for(Long subnet_id : subnet_ids)
                    subnetworks.get(subnet_id).add_commodity(comm);
        }

        return commodities;
    }

    private static Map<KeyCommodityDemandTypeId,AbstractDemandProfile> create_demands_from_jaxb(Network network, jaxb.Demands jaxb_demands) throws OTMException  {
        Map<KeyCommodityDemandTypeId,AbstractDemandProfile> demands = new HashMap<>();
        if (jaxb_demands == null || jaxb_demands.getDemand().isEmpty())
            return demands;
        for (jaxb.Demand jaxb_demand : jaxb_demands.getDemand()) {
            DemandProfile dp = new DemandProfile(jaxb_demand,network);
            KeyCommodityDemandTypeId key = dp.get_key();
            demands.put(key,dp);
        }
        return demands;
    }

    private static void  create_splits_from_jaxb(Network network, jaxb.Splits jaxb_splits) throws OTMException {
        if (jaxb_splits == null || jaxb_splits.getSplitNode().isEmpty())
            return;

        for (jaxb.SplitNode jaxb_split_node : jaxb_splits.getSplitNode()) {

            long node_id = jaxb_split_node.getNodeId();
            long commodity_id = jaxb_split_node.getCommodityId();
            long link_in_id = jaxb_split_node.getLinkIn();

            if(!network.nodes.containsKey(node_id))
                continue;

            Node node = network.nodes.get(node_id);

            KeyCommodityLink key = new KeyCommodityLink(commodity_id,link_in_id);
            float start_time = jaxb_split_node.getStartTime();
            Float dt = jaxb_split_node.getDt();
            SplitMatrixProfile smp = new SplitMatrixProfile(commodity_id,node,link_in_id,start_time,dt);

            for(jaxb.Split jaxb_split : jaxb_split_node.getSplit())
                smp.add_split(jaxb_split);

            node.add_split(key,smp);
        }
    }

    private static boolean have_global_commodity(jaxb.Commodities jc){
        if(jc==null)
            return false;
        for(jaxb.Commodity c : jc.getCommodity())
            if(!c.isPathfull() && (c.getSubnetworks()==null || c.getSubnetworks().isEmpty()))
                return true;
        return false;
    }

    private static double get_pathfull_commodity_flow_through_link(Set<DemandProfile> demands,Set<Subnetwork> my_paths,Link link,float time){

        double total_flow = 0d;

        // loop through all provided paths
        for(Subnetwork subnetwork : my_paths){

            // count how many times the link appears (n=#cycles)
            long n = subnetwork.links.stream()
                    .filter(x->x==link)
                    .count();

            if(n==0)
                continue;

            // add flow for demand that feeds this subnetwork, n times
            total_flow += n*demands.stream()
                    .filter(x->x.path.getId().equals(subnetwork.getId()))
                    .mapToDouble(x->x.profile.get_value_for_time(time))
                    .sum();
        }

        return total_flow;
    }

}