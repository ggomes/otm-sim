package common;

import actuator.AbstractActuator;
import commodity.Commodity;
import commodity.Subnetwork;
import dispatch.EventInitializeController;
import models.AbstractModel;
import models.fluid.AbstractFluidModel;
import runner.RunParameters;
import traveltime.LinkTravelTimeManager;
import control.AbstractController;
import error.OTMErrorLog;
import error.OTMException;
import dispatch.Dispatcher;
import jaxb.Split;
import output.AbstractOutput;
import output.OutputPathTravelTime;
import profiles.*;
import sensor.AbstractSensor;
import utils.OTMUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.util.*;

import static java.util.stream.Collectors.toSet;

public class Scenario {

    public boolean is_initialized;

    public Dispatcher dispatcher;
    public Set<AbstractOutput> outputs = new HashSet<>();

    // Scenario elements
    public Map<Long,Commodity> commodities = new HashMap<>();     // commodity id -> commodity
    public Map<Long,Subnetwork> subnetworks = new HashMap<>();    // subnetwork id -> subnetwork
    public Network network;
    public Map<Long, AbstractController> controllers = new HashMap<>();
    public Map<Long, AbstractActuator> actuators = new HashMap<>();
    public Map<Long, AbstractSensor> sensors = new HashMap<>();

    // commodity/link -> demand profile

    // WHY DO I NEED THIS IN THE SCENARIO?
//    public Map<KeyCommodityDemandTypeId,AbstractDemandProfile> data_demands;

    // travel time computation
    public LinkTravelTimeManager path_tt_manager;

    // available actuators
    public Set<String> available_actuator_types;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public Scenario(){
        this.is_initialized = false;
    }

    public OTMErrorLog validate(){

        OTMErrorLog errorLog =  new OTMErrorLog();

//        if( commodities!=null )
//            commodities.values().forEach(x -> x.validate(errorLog));
        if( subnetworks!=null )
            subnetworks.values().forEach(x -> x.validate(errorLog));
        if( network!=null )
            network.validate(errorLog);
        if( sensors!=null )
            sensors.values().stream().forEach(x -> x.validate(errorLog));
        if( controllers!=null )
            controllers.values().stream().forEach(x -> x.validate(errorLog));
        if( actuators!=null )
            actuators.values().stream().forEach(x -> x.validate(errorLog));

        // check if there are CFL violations, and if so report the max step time
        if(errorLog.haserror()){
            Map<String,Double> maxdt = new HashMap<>();
            for(AbstractModel model : network.models.values())
                maxdt.put(model.name,Double.POSITIVE_INFINITY);
            for(String str : errorLog.getErrors().stream().filter(e->e.description.contains("CFL")).map(e->e.description).collect(toSet())) {
                String[] tokens = str.split(" ");
                long linkid = Long.parseLong(tokens[3]);
                AbstractFluidModel model = (AbstractFluidModel) network.links.get(linkid).model;
                double cfl = Double.parseDouble(tokens[6]);
                maxdt.put(model.name,Math.min(maxdt.get(model.name),model.dt_sec /cfl));
            }
            for(Map.Entry<String,Double> e : maxdt.entrySet())
                errorLog.addError(String.format("The maximum step size for model `%s' is %f",e.getKey(),e.getValue()));
        }

        return errorLog;
    }

    public void initialize(Dispatcher dispatcher) throws OTMException {
        this.initialize(dispatcher,new RunParameters(0f));
    }

    // Use to initialize all of the components of the scenario
    // Use to initialize a scenario that has already been run
    public void initialize(Dispatcher dispatcher,RunParameters runParams) throws OTMException {

        float now = runParams.start_time;

        // attach dispatcher ...............
        this.dispatcher = dispatcher;
        if(dispatcher!=null)
            dispatcher.set_scenario(this);

        // validate the run parameters and outputs
        OTMErrorLog errorLog = new OTMErrorLog();
        runParams.validate(errorLog);

        // validate the outputs
        outputs.stream().forEach(x->x.validate(errorLog));

        // check validation
        errorLog.check();

        // initialize components ..................................
        if(dispatcher!=null)
            dispatcher.initialize(now);

        // To initialize the commodities I will need a map, for each link, from outlink to viable road connections
        Map<Long,Map<Long,Set<RoadConnection>>> link_outlink2rcs = new HashMap<>();
        for(Link link : network.links.values()) {
            Map<Long,Set<RoadConnection>> X = new HashMap<>();
            link_outlink2rcs.put(link.getId(),X);
            for (AbstractLaneGroup lg : link.lanegroups_flwdn)
                for (Map.Entry<Long, RoadConnection> e : lg.outlink2roadconnection.entrySet()) {
                    if (!X.containsKey(e.getKey()))
                        X.put(e.getKey(), new HashSet<>());
                    X.get(e.getKey()).add(e.getValue());
                }
        }

//        for(Commodity commodity : commodities.values())
//            commodity.initialize(this);

        // initialize and register outputs
        for(AbstractOutput x : outputs)
            x.initialize(this);

        // register_with_dispatcher timed writer events
        for(AbstractOutput output : outputs)
            output.register(runParams,dispatcher);

        network.initialize(this,runParams);

        for(AbstractSensor x : sensors.values())
            x.initialize(this);

        for(AbstractController x : controllers.values())
            dispatcher.register_event(new EventInitializeController(dispatcher,x.start_time,x));

        // register initial events ......................................
        if(path_tt_manager!=null)
            path_tt_manager.initialize(dispatcher);

//        network.nodes.values().stream()
//                .filter(node->node.splits!=null)
//                .flatMap(node->node.splits.values().stream())
//                .forEach(x->x.register_with_dispatcher(dispatcher));

        is_initialized = true;
    }

    ///////////////////////////////////////////////////
    // export
    ///////////////////////////////////////////////////

    public void to_xml(String filename){
        try {
            File file = new File(filename);
            JAXBContext jaxbContext = JAXBContext.newInstance(jaxb.Scenario.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(to_jaxb(), file);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public jaxb.Scenario to_jaxb(){

        jaxb.Scenario jsc = new jaxb.Scenario();

        // comodities
        jaxb.Commodities jcomms = new jaxb.Commodities();
        jsc.setCommodities(jcomms);
        for(commodity.Commodity comm : commodities.values())
            jcomms.getCommodity().add(comm.to_jaxb());

        // network
        jsc.setNetwork(network.to_jaxb());

        // subnetworks
        jaxb.Subnetworks jsubs = new jaxb.Subnetworks();
        jsc.setSubnetworks(jsubs);
        for(commodity.Subnetwork subnetwork : subnetworks.values())
            if(subnetwork.getId()!=0l)
                jsubs.getSubnetwork().add(subnetwork.to_jaxb());

        // demands
        jaxb.Demands jdems = new jaxb.Demands();
        jsc.setDemands(jdems);
        for(Link link : network.links.values()){
            if(link.demandGenerators ==null || link.demandGenerators.isEmpty())
                continue;
            for(AbstractDemandGenerator gen : link.demandGenerators){

                commodity.Commodity comm = gen.commodity;

                jaxb.Demand jdem = new jaxb.Demand();
                jdems.getDemand().add(jdem);

                if(comm.pathfull)
                    jdem.setSubnetwork(gen.path.getId());
                else
                    jdem.setLinkId(gen.link.getId());

                jdem.setContent(OTMUtils.comma_format(OTMUtils.times(gen.profile.values,3600d)));
                jdem.setDt(gen.profile.dt);
                jdem.setCommodityId(comm.getId());
                jdem.setStartTime(gen.profile.start_time);
            }
        }

        // splits
        jaxb.Splits jsplits = new jaxb.Splits();
        jsc.setSplits(jsplits);
        for(common.Link link : network.links.values()){
            if(link.split_profile!=null){
                for(Map.Entry<Long, SplitMatrixProfile> e : link.split_profile.entrySet()){
                    Long commodity_id = e.getKey();
                    SplitMatrixProfile profile = e.getValue();

                    if(profile==null || profile.splits==null)
                        continue;

                    jaxb.SplitNode jspltnode = new jaxb.SplitNode();
                    jsplits.getSplitNode().add(jspltnode);

                    jspltnode.setCommodityId(commodity_id);
                    jspltnode.setDt(profile.get_dt());
                    jspltnode.setStartTime(profile.get_start_time());
                    jspltnode.setLinkIn(profile.link_in.getId());
                    jspltnode.setNodeId(link.end_node.getId());

                    List<Split> splitlist = jspltnode.getSplit();
                    for(Map.Entry<Long,List<Double>> e1 : profile.get_outlink_to_profile().entrySet()){
                        jaxb.Split split = new jaxb.Split();
                        splitlist.add(split);
                        split.setLinkOut(e1.getKey());
                        split.setContent( OTMUtils.comma_format(e1.getValue()));
                    }
                }
            }
        }

        // controllers
        jaxb.Controllers jctrls = new jaxb.Controllers();
        jsc.setControllers(jctrls);
        for(AbstractController absctrl : controllers.values()){
            jaxb.Controller jctrl = new jaxb.Controller();
            jctrls.getController().add(jctrl);

            jctrl.setId(absctrl.id);
            jctrl.setType(absctrl.type.toString());

            jctrl.setDt(absctrl.dt);
//            jctrl.setSchedule();

//            List<Long> acts = absctrl.actuators.stream().map(x->x.id).collect(Collectors.toList());
//            jctrl.setTargetActuators(OTMUtils.comma_format(acts));
        }

        // actuators
        jaxb.Actuators jacts = new jaxb.Actuators();
        jsc.setActuators(jacts);
        for(AbstractActuator absact : actuators.values()){
            jaxb.Actuator jact = new jaxb.Actuator();
            jacts.getActuator().add(jact);

            jact.setId(absact.id);

            jaxb.ActuatorTarget target = new jaxb.ActuatorTarget();
            jact.setActuatorTarget(target);
            target.setType(absact.target.getTypeAsTarget());
            target.setId(String.format("%d",absact.target.getIdAsTarget()));
            jact.setType(absact.getType().toString());
        }


        // TODO: fix this
//        // sensors
//        jaxb.Sensors jsnsrs = new jaxb.Sensors();
//        jsc.setSensors(jsnsrs);
//        for(AbstractSensor abssns : sensors.values()){
//            Long link_id = abssns.link.getId();
//
//            jaxb.Sensor jsns = new jaxb.Sensor();
//            jsnsrs.getSensor().add(jsns);
//            jsns.setId(abssns.id);
//            jsns.setDt(abssns.dt);
////            jsns.setLength(abssns.);
//            jsns.setLinkId(link_id);
////            jsns.setDataId(abssns);
//            jsns.setLanes(abssns.start_lane + "#" + abssns.end_lane);
//            jsns.setPosition(abssns.flwpos);
//            jsns.setType(abssns.type.toString());
//
//        }

        return jsc;

    }

    ///////////////////////////////////////////////////
    // get
    ///////////////////////////////////////////////////

    public float get_current_time(){
        return dispatcher.current_time;
    }

    public InterfaceScenarioElement get_element(ScenarioElementType type, Long id){

        switch(type){
            case commodity:
                return commodities.get(id);
            case node:
                return network.nodes.get(id);
            case link:
                return network.links.get(id);
            case roadconnection:
                return network.get_road_connection(id);
            case controller:
                return controllers.get(id);
            case actuator:
                return actuators.get(id);
            case sensor:
                return sensors.get(id);
            default:
                System.err.println("Bad element type in get_element");
        }
        return null;
    }

    public Set<Profile1D> get_demands_for_commodity(Long commodity_id){
        return network.links.values().stream()
                .filter(link->link.demandGenerators!=null)
                .flatMap(link->link.demandGenerators.stream())
                .filter(gen->commodity_id==gen.commodity.getId())
                .map(s->s.profile)
                .collect(toSet());
    }

    public Set<Profile1D> get_demands_for_link(Long link_id){
        Link link = this.network.links.get(link_id);
        if(link==null)
            return null;
        return link.demandGenerators.stream().map(z->z.profile).collect(toSet());
    }

    ///////////////////////////////////////////////////
    // run
    ///////////////////////////////////////////////////

    public void end_run() {
        try {
            for(AbstractOutput or : outputs)
                or.close();
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    ///////////////////////////////////////////////////
    // travel time manager
    ///////////////////////////////////////////////////

    public void add_path_travel_time(OutputPathTravelTime path_tt_writer) throws OTMException {
        if(path_tt_manager==null)
            path_tt_manager = new LinkTravelTimeManager(this);

        path_tt_manager.add_path_travel_time_writer(path_tt_writer);
    }


    ///////////////////////////////////////////////////
    // private
    ///////////////////////////////////////////////////

//    private Map<Long,Map<Long,Set<RoadConnection>>> build_link_to_outrc_map(){
//
////        // I will need a map from links to outgoing road connections to initialize commodities.
////        // First build set of used links
////        Set<Link> used_links = commodities.values().stream()
////                .flatMap(c->c.subnetworks.stream())
////                .flatMap(s->s.get_links().stream())
////                .collect(Collectors.toSet());
//
//        // make temporary map from links to outgoing road connections
//        Map<Long,Map<Long,Set<RoadConnection>>> link_outlink2rcs = new HashMap<>();
//        for(Link link : used_links) {
//            Map<Long,Set<RoadConnection>> X = new HashMap<>();
//            link_outlink2rcs.put(link.getId(),X);
//            for (AbstractLaneGroup lg : link.lanegroups_flwdn.values())
//                for (Map.Entry<Long, RoadConnection> e : lg.outlink2roadconnection.entrySet()) {
//                    if (!X.containsKey(e.getKey()))
//                        X.put(e.getKey(), new HashSet<>());
//                    X.get(e.getKey()).add(e.getValue());
//                }
//        }
//
//        return link_outlink2rcs;
//    }

}
