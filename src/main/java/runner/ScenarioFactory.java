package runner;

import actuator.*;
import actuator.ActuatorSignal;
import commodity.*;
import commodity.Commodity;
import commodity.Subnetwork;
import common.*;
import control.*;
import control.commodity.ControllerRestrictLaneGroup;
import control.commodity.ControllerOfframpFlow;
import control.commodity.ControllerTollLaneGroup;
import control.rampmetering.*;
import control.sigint.ControllerSignalPretimed;
import error.OTMErrorLog;
import error.OTMException;
import keys.KeyCommodityDemandTypeId;
import plugin.PluginLoader;
import profiles.*;
import sensor.AbstractSensor;
import sensor.FixedSensor;
import utils.OTMUtils;

import java.util.*;

import static java.util.stream.Collectors.toSet;

public class ScenarioFactory {

    ///////////////////////////////////////////
    // public
    ///////////////////////////////////////////

    public static Scenario create_scenario(jaxb.Scenario js, boolean validate) throws OTMException {

        OTMUtils.reset_counters();

        Scenario scenario = new Scenario();

        // plugins ..........................................................
        PluginLoader.load_plugins( js.getPlugins() );

        // network ...........................................................
        scenario.network = ScenarioFactory.create_network_from_jaxb(scenario, js.getCommodities(),js.getModels(), js.getNetwork());

        // commodities ......................................................
        scenario.subnetworks = ScenarioFactory.create_subnetworks_from_jaxb(
                scenario.network,
                js.getSubnetworks() ,
                have_global_commodity(js.getCommodities()) );

        scenario.commodities = ScenarioFactory.create_commodities_from_jaxb(
                scenario,
                scenario.subnetworks,
                js.getCommodities());

        // control ...............................................
        scenario.actuators = ScenarioFactory.create_actuators_from_jaxb(scenario, js.getActuators() );
        scenario.sensors = ScenarioFactory.create_sensors_from_jaxb(scenario, js.getSensors() );
        scenario.controllers = ScenarioFactory.create_controllers_from_jaxb(scenario,js.getControllers() );

        // populate link.path2outlink (requires commodities)
        Set<Subnetwork> used_paths = scenario.commodities.values().stream()
                .filter(c->c.pathfull)
                .map(c->c.subnetworks)
                .flatMap(c->c.stream())
                .collect(toSet());

        for(Subnetwork subnet : used_paths){
            if(!subnet.isPath())
                throw new OTMException(String.format("ERROR: Subnetwork %d is assigned to a pathfull commodity, but it is not a linear path",subnet.getId()));
            Path path = (Path) subnet;
            for(int i=0;i<path.ordered_links.size()-1;i++){
                Link link = path.ordered_links.get(i);
                Link next_link = path.ordered_links.get(i+1);
                link.path2outlink.put(path.getId(),next_link);
            }
        }

        // allocate the state ..............................................
        for(Commodity commodity : scenario.commodities.values())
            if(commodity.pathfull)
                for(Subnetwork subnetwork : commodity.subnetworks)
                    for(Link link : subnetwork.get_links())
                        commodity.register_commodity(link,commodity,subnetwork);
            else
                for(Link link : scenario.network.links.values())
                    commodity.register_commodity(link, commodity, null);

        // lane change models .............................
        assign_lane_change_models(scenario.commodities,scenario.network.links,js.getLanechanges());

        // splits ...........................................................
        ScenarioFactory.create_splits_from_jaxb(scenario.network, js.getSplits());

        // demands ..........................................................
        scenario.data_demands = ScenarioFactory.create_demands_from_jaxb(scenario.network, js.getDemands());

        // validate ................................................
        if(validate) {
            OTMErrorLog errorLog = scenario.validate();
            errorLog.check();
        }

        return scenario;
    }

    public static Scenario create_unrunnable_scenario(jaxb.Scenario js) throws OTMException {

        OTMUtils.reset_counters();

        Scenario scenario = new Scenario();

        // common ...........................................................
        scenario.network = new common.Network(
                scenario ,
                js.getCommodities().getCommodity(),
                js.getNetwork().getNodes().getNode(),
                js.getNetwork().getLinks().getLink(),
                js.getNetwork().getRoadparams() );

        scenario.subnetworks = ScenarioFactory.create_subnetworks_from_jaxb(
                scenario.network,
                js.getSubnetworks() ,
                have_global_commodity(js.getCommodities()) );

        // commodities
        scenario.commodities = new HashMap<>();
        if(js.getCommodities()!=null)
            for (jaxb.Commodity jaxb_comm : js.getCommodities().getCommodity())
                scenario.commodities.put( jaxb_comm.getId(),  new Commodity( jaxb_comm,null,scenario) );

        // OD node map
        Map<Long,Long> origin_nodes = new HashMap<>();
        Map<Long,Long> destination_nodes = new HashMap<>();
        if(js.getSubnetworks()!=null)
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
                KeyCommodityDemandTypeId key = new KeyCommodityDemandTypeId(comm.getId(),subnetwork_id,comm.get_demand_type());
                scenario.data_demands.put(key,dp);
            }

        return scenario;
    }

    public static AbstractController create_controller_from_jaxb(Scenario scenario, jaxb.Controller jaxb_controller) throws OTMException {
        AbstractController controller;
        AbstractController.Algorithm type = AbstractController.Algorithm.valueOf(jaxb_controller.getType());
        switch(type){
            case schedule:
                controller = new ControllerSchedule(scenario,jaxb_controller);
                break;
            case sig_pretimed:
                controller = new ControllerSignalPretimed(scenario,jaxb_controller);
                break;
            case rm_alinea:
                controller = new ControllerAlinea(scenario,jaxb_controller);
                break;
            case rm_fixed_rate:
                controller = new ControllerFixedRate(scenario,jaxb_controller);
                break;
            case rm_open:
                controller = new ControllerRampMeterOpen(scenario,jaxb_controller);
                break;
            case rm_closed:
                controller = new ControllerRampMeterClosed(scenario,jaxb_controller);
                break;
            case lg_restrict:
                controller = new ControllerRestrictLaneGroup(scenario,jaxb_controller);
                break;
            case lg_toll:
                controller = new ControllerTollLaneGroup(scenario,jaxb_controller);
                break;
            case frflow:
                controller = new ControllerOfframpFlow(scenario,jaxb_controller);
                break;
            default:

                // it might be a plugin
                controller = PluginLoader.get_controller_instance(jaxb_controller.getType(),scenario,jaxb_controller);

                if(controller==null)
                    throw new OTMException("Bad controller type: " + jaxb_controller.getType());
                break;
        }

        return controller;
    }

    ///////////////////////////////////////////
    // private static
    ///////////////////////////////////////////

    private static common.Network create_network_from_jaxb(Scenario scenario, jaxb.Commodities jaxb_comms,jaxb.Models jaxb_models,jaxb.Network jaxb_network) throws OTMException {
        common.Network network = new common.Network(
                scenario ,
                jaxb_comms==null ? null : jaxb_comms.getCommodity(),
                jaxb_models==null ? null : jaxb_models.getModel(),
                jaxb_network.getNodes(),
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
            if(sensors.containsKey(jaxb_sensor.getId()))
                throw new OTMException("Duplicate sensor id found: " + jaxb_sensor.getId());
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
            if(actuators.containsKey(jaxb_actuator.getId()))
                throw new OTMException("Duplicate actuator id found: " + jaxb_actuator.getId());


            AbstractActuator.Type type = AbstractActuator.Type.valueOf(jaxb_actuator.getType());
            switch(type){
                case meter:
                    actuator = new ActuatorMeter(scenario,jaxb_actuator);
                    break;
                case signal:
                    actuator = new ActuatorSignal(scenario,jaxb_actuator);
                    break;
                case stop:
                    actuator = new ActuatorStop(scenario,jaxb_actuator);
                    break;
                case lg_restrict:
                    actuator = new ActuatorOpenCloseLaneGroup(scenario,jaxb_actuator);
                    break;
                case lg_lanechange:
                    actuator = new ActuatorLaneChange(scenario,jaxb_actuator);
                    break;
                case lg_speed:
                    throw new OTMException("NOT IMPLEMENTED YET");
                case split:
                    actuator = new ActuatorSplit(scenario,jaxb_actuator);
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
        for(jaxb.Controller jaxb_controller : jaxb_controllers.getController())
            controllers.put(jaxb_controller.getId(),create_controller_from_jaxb(scenario,jaxb_controller));
        return controllers;
    }

    private static Map<Long, Subnetwork> create_subnetworks_from_jaxb(Network network, jaxb.Subnetworks jaxb_subnets,boolean have_global_commodity) throws OTMException {

        HashMap<Long, Subnetwork> subnetworks = new HashMap<>();

        if(jaxb_subnets!=null && jaxb_subnets.getSubnetwork().stream().anyMatch(x->x.getId()==0L))
            throw new OTMException("Subnetwork id '0' is not allowed.");

        // create global commodity
//        if(have_global_commodity) {
//            Subnetwork subnet = new Subnetwork(network);
//            subnetworks.put(0l, subnet.isPath() ? new Path(network) : subnet );
//        }

        if ( jaxb_subnets != null ){
            // initialize
            for (jaxb.Subnetwork jaxb_subnet : jaxb_subnets.getSubnetwork()) {
                if (subnetworks.containsKey(jaxb_subnet.getId()))
                    throw new OTMException("Repeated subnetwork id");
                Subnetwork subnet = new Subnetwork(jaxb_subnet,network);
//                subnetworks.put(jaxb_subnet.getId(), subnet.is_path ? new Path(jaxb_subnet,network) : subnet );
                subnetworks.put(jaxb_subnet.getId(), subnet.isPath() ? new Path(subnet) : subnet );
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

    private static void assign_lane_change_models(Map<Long,Commodity> comms,Map<Long,Link> links,jaxb.Lanechanges jlcs) throws OTMException {

        String default_type = "keep";
        float default_dt = 0f;

        if(jlcs==null) {
            for(Link link : links.values())
                for(AbstractLaneGroup lg : link.lanegroups_flwdn.values())
                    lg.assign_lane_selector(default_type,default_dt,null,comms.keySet());
            return;
        }

        Set<Long> unassigned = new HashSet<>(links.keySet());
        for(jaxb.Lanechange lc : jlcs.getLanechange()){
            String type = lc.getType();
            if(lc.getLinks()==null)
                continue;
            Collection<Long> linkids = lc.getLinks()==null ? links.keySet() : OTMUtils.csv2longlist(lc.getLinks());
            Collection<Long> commids = lc.getComms()==null ? comms.keySet() : OTMUtils.csv2longlist(lc.getComms());
            unassigned.removeAll(linkids);
            for(Long linkid : linkids)
                if(links.containsKey(linkid))
                    for(AbstractLaneGroup lg : links.get(linkid).lanegroups_flwdn.values())
                        lg.assign_lane_selector(type,lc.getDt(),lc.getParameters(),commids);
        }

        if(!unassigned.isEmpty()){
            Optional<jaxb.Lanechange> x = jlcs.getLanechange().stream().filter(xx -> xx.isIsDefault()).findFirst();
            String my_default_type = x.isPresent() ? x.get().getType() : default_type;
            float my_dt = x.isPresent() ? x.get().getDt() : default_dt;
            jaxb.Parameters my_params = x.isPresent() ? x.get().getParameters() : null;
            for(Long linkid : unassigned)
                for(AbstractLaneGroup lg : links.get(linkid).lanegroups_flwdn.values())
                    lg.assign_lane_selector(my_default_type,my_dt,my_params,comms.keySet());
        }

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
            long commodity_id = jaxb_split_node.getCommodityId();
            long link_in_id = jaxb_split_node.getLinkIn();

            if(!network.links.containsKey(link_in_id))
                continue;
            Link link_in = network.links.get(link_in_id);

            if(!link_in.split_profile.containsKey(commodity_id))
                continue;

            SplitMatrixProfile smp = link_in.split_profile.get(commodity_id);

            float start_time = jaxb_split_node.getStartTime();
            Float dt = jaxb_split_node.getDt();
            smp.splits = new Profile2D(start_time,dt);

            for(jaxb.Split jaxb_split : jaxb_split_node.getSplit()) {
                long linkout_id = jaxb_split.getLinkOut();
                if(network.links.containsKey(linkout_id))
                    smp.splits.add_entry(linkout_id,  jaxb_split.getContent() );
            }


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
            long n = subnetwork.get_links().stream()
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