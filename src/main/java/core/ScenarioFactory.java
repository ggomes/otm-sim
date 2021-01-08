package core;

import actuator.*;
import actuator.ActuatorSignal;
import commodity.*;
import commodity.Commodity;
import commodity.Subnetwork;
import control.*;
import control.commodity.ControllerFlowToLinks;
import control.commodity.ControllerRestrictLaneGroup;
import control.commodity.ControllerOfframpFlow;
import control.commodity.ControllerTollLaneGroup;
import control.rampmetering.*;
import control.sigint.ControllerSignalPretimed;
import error.OTMErrorLog;
import error.OTMException;
import lanechange.LinkLaneSelector;
import models.fluid.ctm.ModelCTM;
import models.none.ModelNone;
import models.vehicle.newell.ModelNewell;
import models.vehicle.spatialq.ModelSpatialQ;
import plugin.PluginLoader;
import profiles.*;
import sensor.AbstractSensor;
import sensor.FixedSensor;
import utils.OTMUtils;
import utils.StochasticProcess;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

public class ScenarioFactory {

    ///////////////////////////////////////////
    // public
    ///////////////////////////////////////////

    public static Scenario create_scenario(jaxb.Scenario js, boolean validate_pre_init) throws OTMException {

        OTMUtils.reset_counters();

        Scenario scenario = new Scenario();

        // plugins
        PluginLoader.load_plugins( js.getPlugins() );

        // network and subnetworks
        ScenarioFactory.create_network_and_subnetworks_from_jaxb(scenario, js.getNetwork(), js.getSubnetworks());

        // commodities
        scenario.commodities = ScenarioFactory.create_commodities_from_jaxb(scenario.subnetworks, js.getCommodities());

        // splits
        ScenarioFactory.create_splits_from_jaxb(scenario.network, scenario.commodities, js.getSplits());

        // demands
        ScenarioFactory.create_demands_from_jaxb(scenario, js.getDemands());

        // control
        scenario.sensors = ScenarioFactory.create_sensors_from_jaxb(scenario, js.getSensors() );
        scenario.actuators = ScenarioFactory.create_actuators_from_jaxb(scenario, js.getActuators() );
        scenario.controllers = ScenarioFactory.create_controllers_from_jaxb(scenario,js.getControllers() );

        // generate models
        scenario.models = create_models_from_jaxb(scenario,js.getModels());

        // validate
        if(validate_pre_init) {
            OTMErrorLog errorLog = scenario.validate_pre_init();
            errorLog.check();
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
            case linkflow:
                controller = new ControllerFlowToLinks(scenario,jaxb_controller);
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
    // private
    ///////////////////////////////////////////

    private static void create_network_and_subnetworks_from_jaxb(Scenario scenario, jaxb.Network jaxb_network, jaxb.Subnetworks jaxb_subnetworks) throws OTMException {

        if(jaxb_subnetworks!=null && jaxb_subnetworks.getSubnetwork().stream().anyMatch(x->x.getId()==0L))
            throw new OTMException("Subnetwork id '0' is not allowed.");

        scenario.subnetworks = new HashMap<>();
        if ( jaxb_subnetworks != null ){
            for (jaxb.Subnetwork jaxb_subnet : jaxb_subnetworks.getSubnetwork()) {
                if (scenario.subnetworks.containsKey(jaxb_subnet.getId()))
                    throw new OTMException("Repeated subnetwork id");
                scenario.subnetworks.put(jaxb_subnet.getId(), jaxb_subnet.isIsroute() ? new Path(jaxb_subnet) : new Subnetwork(jaxb_subnet) );
            }
        }

        scenario.network = new core.Network(
                scenario ,
                jaxb_network.getNodes(),
                jaxb_network.getLinks().getLink(),
                jaxb_network.getRoadgeoms() ,
                jaxb_network.getRoadconnections() ,
                jaxb_network.getRoadparams() );

        for(Subnetwork subnetwork : scenario.subnetworks.values()) {
            if (subnetwork instanceof Path) {
                Path path = (Path) subnetwork;

                // populate ordered links
                if (!path.populate_ordered_links(scenario.network))
                    throw new OTMException(String.format("Subnetwork %d is not a path.", subnetwork.getId()));

                // link.path2outlink
                for (int i = 0; i < path.ordered_links.size() - 1; i++) {
                    Link link = path.ordered_links.get(i);
                    Link next_link = path.ordered_links.get(i + 1);
                    link.path2outlink.put(path.getId(), next_link);
                }
            }
        }

    }

    private static Map<String, AbstractModel> create_models_from_jaxb(Scenario scenario,jaxb.Models jMs) throws OTMException {

        Map<String, AbstractModel> models = new HashMap<>();

        if(jMs==null)
            return models;

        List<jaxb.Model> jms = jMs.getModel();

        // throw jmodels into a set
        Set<jaxb.Model> jmodels = new HashSet<>(jms);

        // duplicate names
        Set<String> names = jmodels.stream().map(x->x.getName()).collect(toSet());
        if( names.size()!=jmodels.size())
            throw new OTMException("There are duplicate model names.");

        // has default
        Set<String> defs = jmodels.stream()
                .filter(x->x.isIsDefault())
                .map(x->x.getName())
                .collect(Collectors.toSet());

        if(defs.size()>1)
            throw new OTMException("Multiple defaults.");

        Set<Link> assigned_links = new HashSet<>();
        for(jaxb.Model jmodel : jmodels ){
            AbstractModel model = create_model(scenario,jmodel);
            models.put(jmodel.getName(),model);
            assigned_links.addAll(model.links);
        }

        // assign 'none' model to remaining links
        if(assigned_links.size()<scenario.network.links.size()){

            if(models.containsKey("none"))
                throw new OTMException("'none' is a prohibited name for a model.");

            Set<Link> my_links = new HashSet<>();
            my_links.addAll(scenario.network.links.values());
            my_links.removeAll(assigned_links);
            models.put("none", new ModelNone("none",my_links));
        }

        return models;
    }

    protected static AbstractModel create_model(Scenario scenario,jaxb.Model jmodel) throws OTMException {

        StochasticProcess process;
        try {
            process = jmodel.getProcess()==null ? StochasticProcess.poisson : StochasticProcess.valueOf(jmodel.getProcess());
        } catch (IllegalArgumentException e) {
            process = StochasticProcess.poisson;
        }

        // links for this model
        Set<Link> my_links = jmodel.isIsDefault() ? new HashSet<>(scenario.network.links.values()) :
                OTMUtils.csv2longlist(jmodel.getLinks()).stream()
                .map( linkid -> scenario.network.links.get(linkid) )
                .collect(toSet());

        if(my_links.stream().anyMatch(x->x==null))
            throw new OTMException("Unknown link id in model " + jmodel.getName());

        AbstractModel model;
        switch(jmodel.getType()){
            case "ctm":
                model = new ModelCTM(jmodel.getName(),
                        my_links,
                        process,
                        jmodel.getModelParams());
                break;

            case "spaceq":
                model = new ModelSpatialQ(jmodel.getName(),
                        my_links,
                        process);
                break;

            case "micro":
                model = new ModelNewell(jmodel.getName(),
                        my_links,
                        process,
                        jmodel.getModelParams());
                break;

            case "none":
                model = new ModelNone(jmodel.getName() ,
                        my_links);
                break;

            default:

                // it might be a plugin
                model = PluginLoader.get_model_instance(jmodel,process);

                if(model==null)
                    throw new OTMException("Bad model type: " + jmodel.getType());
                break;

        }

        model.configure(scenario,jmodel.getLanechanges() );

        return model;
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
                case lg_capacity:
                    actuator = new ActuatorLaneGroupCapacity(scenario,jaxb_actuator);
                    break;
                case signal:
                    actuator = new ActuatorSignal(scenario,jaxb_actuator);
                    break;
                case lg_allowcomm:
                    actuator = new ActuatorLaneGroupAllowComm(scenario,jaxb_actuator);
                    break;
                case lg_speedlimit:
                    throw new OTMException("NOT IMPLEMENTED YET");
                case split:
                    actuator = new ActuatorSplit(scenario,jaxb_actuator);
                    break;
                case flowtolink:
                    actuator = new ActuatorFlowToLinks(scenario,jaxb_actuator);
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

    private static Map<Long, Commodity> create_commodities_from_jaxb(Map<Long, Subnetwork> subnetworks, jaxb.Commodities jaxb_commodities) throws OTMException {

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

            Commodity comm = new Commodity( jaxb_comm,subnet_ids,subnetworks);
            commodities.put( jaxb_comm.getId(), comm );

            // inform the subnetwork of their commodities
            if(!is_global)
                for(Long subnet_id : subnet_ids)
                    subnetworks.get(subnet_id).add_commodity(comm);
        }

        return commodities;
    }

    private static void create_demands_from_jaxb(Scenario scenario, jaxb.Demands jaxb_demands) throws OTMException  {
        if (jaxb_demands == null || jaxb_demands.getDemand().isEmpty())
            return;
        for (jaxb.Demand jd : jaxb_demands.getDemand()) {

            if(!scenario.commodities.containsKey(jd.getCommodityId()))
                throw new OTMException("Bad commodity in demands");

            Commodity comm = scenario.commodities.get(jd.getCommodityId());
            Path path = null;
            Long linkid;
            if(comm.pathfull){
                if(jd.getSubnetwork()==null || !scenario.subnetworks.containsKey(jd.getSubnetwork()))
                    throw new OTMException("Bad subnetwork id (" + jd.getSubnetwork() + ") in demand for commodity " + comm.getId());
                Subnetwork subnetwork = scenario.subnetworks.get(jd.getSubnetwork());
                if(!(subnetwork instanceof Path))
                    throw new OTMException("Subnetwork is not a path: id " + jd.getSubnetwork() + ", in demand for commodity " + comm.getId());
                path = (Path)subnetwork;
                linkid = path.get_origin().getId();
            } else {
                if(jd.getLinkId()==null || !scenario.network.links.containsKey(jd.getLinkId()))
                    throw new OTMException("Bad link id (" + jd.getLinkId() + ") in demand for commodity " + comm.getId());
                linkid = jd.getLinkId();
            }

            Profile1D profile = new Profile1D(jd.getStartTime(), jd.getDt(),OTMUtils.csv2list(jd.getContent()));
            profile.multiply(1.0/3600.0);

            Set<DemandInfo> demandinfos;
            if(!scenario.demands.containsKey(linkid)) {
                demandinfos = new HashSet<>();
                scenario.demands.put(linkid,demandinfos);
            }
            else
                demandinfos = scenario.demands.get(linkid);
            demandinfos.add(new DemandInfo(comm.getId(),path==null?null:path.getId(),profile));

        }
    }

    private static void  create_splits_from_jaxb(Network network, Map<Long,Commodity> commodities, jaxb.Splits jaxb_splits) throws OTMException {

        if (jaxb_splits == null || jaxb_splits.getSplitNode().isEmpty())
            return;

        // allocate split matrix
        Set<Long> pathless_comms = commodities.values().stream()
                .filter(c->!c.pathfull)
                .map(c->c.getId())
                .collect(toSet());

        network.links.values().stream()
                .filter(link -> !link.is_sink && link.end_node.out_links.size()>1)
                .forEach(link -> link.allocate_splits(pathless_comms));

        // split values from jaxb
        for (jaxb.SplitNode jaxb_split_node : jaxb_splits.getSplitNode()) {
            long commodity_id = jaxb_split_node.getCommodityId();
            long link_in_id = jaxb_split_node.getLinkIn();

            if(!network.links.containsKey(link_in_id))
                continue;

            Link link_in = network.links.get(link_in_id);

            if(link_in.split_profile==null || !link_in.split_profile.containsKey(commodity_id))
                continue;

            SplitMatrixProfile smp = link_in.split_profile.get(commodity_id);

            float start_time = jaxb_split_node.getStartTime();
            Float dt = jaxb_split_node.getDt();
            smp.set_splits( new Profile2D(start_time,dt) );

            for(jaxb.Split jaxb_split : jaxb_split_node.getSplit()) {
                long linkout_id = jaxb_split.getLinkOut();
                if(network.links.containsKey(linkout_id))
                    smp.get_splits().add_entry(linkout_id,  jaxb_split.getContent() );
            }
        }
    }

}