package core;

import actuator.AbstractActuator;
import commodity.Commodity;
import commodity.Path;
import commodity.Subnetwork;
import dispatch.EventInitializeController;
import cmd.RunParameters;
import events.AbstractScenarioEvent;
import output.animation.AnimationInfo;
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


/**
 * Methods for extracting information and manipulating the scenario.
 * This is the main class of the simulation. It contains references to all of the components of the simulation.
 * Scenario elements can be retrieved from maps using their ids. Each scenario element has its own API.
 * You should only interact with the scenario elements through the docuemented API. There are additional public
 * methods, but these should not be used if they are not part of the documented API.
 */
public class Scenario {

    public Dispatcher dispatcher;
    public Set<AbstractOutput> outputs = new HashSet<>();

    /** Container for the network **/
    public Network network;

    /** Map of models **/
    public Map<String,AbstractModel> models;

    /** Map of commodities**/
    public Map<Long,Commodity> commodities = new HashMap<>();     // commodity id -> commodity

    /** Map of subnetworks **/
    public Map<Long,Subnetwork> subnetworks = new HashMap<>();    // subnetwork id -> subnetwork

    /** Map of controllers  **/
    public Map<Long, AbstractController> controllers = new HashMap<>();

    /** Map of actuators **/
    public Map<Long, AbstractActuator> actuators = new HashMap<>();

    /** Map of sensors **/
    public Map<Long, AbstractSensor> sensors = new HashMap<>();

    /** Map of demands **/
    public Map<Long, Set<DemandInfo>> demands = new HashMap<>(); // link id -> DemandInfo

    /** Map of events **/
    public Map<Long, AbstractScenarioEvent> events = new HashMap<>(); // event id -> AbstractEvent

    // travel time computation
    public LinkTravelTimeManager path_tt_manager;

    ///////////////////////////////////////////////////
    // validate / initialize
    ///////////////////////////////////////////////////

    protected OTMErrorLog validate_pre_init(){

        OTMErrorLog errorLog =  new OTMErrorLog();

        commodities.values().forEach(x->x.validate_pre_init(errorLog));
        network.links.values().forEach(x->x.validate_pre_init(errorLog));
        network.road_geoms.values().forEach(x->x.validate_pre_init(errorLog));
        network.road_connections.values().forEach(x->x.validate_pre_init(errorLog));

        if(models!=null)
            models.values().stream().forEach(x -> x.validate_pre_init(errorLog));
        if( subnetworks!=null )
            subnetworks.values().forEach(x -> x.validate_pre_init(errorLog));
        if( controllers!=null )
            controllers.values().stream().forEach(x -> x.validate_pre_init(errorLog));
        if( actuators!=null )
            actuators.values().stream().forEach(x -> x.validate_pre_init(errorLog));
        if(events!=null)
            events.values().stream().forEach(x->x.validate_pre_init(errorLog));

        // check if there are CFL violations, and if so report the max step time
        if(errorLog.haserror()){

            if( errorLog.getErrors().stream().anyMatch(e->e.description.contains("CFL")) ){
                Map<String,Double> maxdt = new HashMap<>();
                for(AbstractModel model : models.values())
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

        }

        return errorLog;
    }

    protected void initialize(Dispatcher dispatcher) throws OTMException {
        this.initialize(dispatcher,new RunParameters(0f),true);
    }

    protected void initialize(Dispatcher dispatcher,RunParameters runParams,boolean validate_post_init) throws OTMException {

        // attach dispatcher ...............
        this.dispatcher = dispatcher;
        if(dispatcher!=null)
            dispatcher.set_scenario(this);

        // validate the run parameters and outputs
        OTMErrorLog errorLog1 = new OTMErrorLog();
        runParams.validate(errorLog1);

        // check validation
        errorLog1.check();

        // initialize components ..................................
        if(dispatcher!=null)
            dispatcher.initialize();

        // initialize and register outputs
        for(AbstractOutput x : outputs)
            x.initialize(this);

        // register_with_dispatcher timed writer events
        for(AbstractOutput output : outputs)
            output.register(runParams,dispatcher);

        network.initialize(this,runParams.start_time);

        for(AbstractModel model : models.values())
            model.initialize(this,runParams.start_time);

        for(AbstractSensor x : sensors.values())
            x.initialize(this);

        for(AbstractController x : controllers.values()) {
            float start_time = Math.max( x.start_time , runParams.start_time );
            dispatcher.register_event(new EventInitializeController(dispatcher, start_time, x));
        }

        for(AbstractScenarioEvent event: events.values())
            event.initialize(this);

        if(path_tt_manager!=null)
            path_tt_manager.initialize(dispatcher);

        // validate
        if(validate_post_init) {
            OTMErrorLog errorLog2 = validate_post_init();
            errorLog2.check();
        }

    }

    private OTMErrorLog validate_post_init(){

        OTMErrorLog errorLog =  new OTMErrorLog();

        outputs.stream().forEach(x->x.validate_post_init(errorLog));

        network.links.values().forEach(x->x.validate_post_init(errorLog));
        network.road_geoms.values().forEach(x->x.validate_post_init(errorLog));
        network.road_connections.values().forEach(x->x.validate_post_init(errorLog));

        if(models!=null)
            models.values().stream().forEach(x -> x.validate_post_init(errorLog));
        if( subnetworks!=null )
            subnetworks.values().forEach(x -> x.validate_post_init(errorLog));
        if( sensors!=null )
            sensors.values().stream().forEach(x -> x.validate_post_init(errorLog));

        if(events!=null)
            events.values().stream().forEach(x->x.validate_post_init(errorLog));

        // check if there are CFL violations, and if so report the max step time
        if(errorLog.haserror()){

            if( errorLog.getErrors().stream().anyMatch(e->e.description.contains("CFL")) ){
                Map<String,Double> maxdt = new HashMap<>();
                for(AbstractModel model : models.values())
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

        }

        return errorLog;
    }

    ///////////////////////////////////////////////////
    // export
    ///////////////////////////////////////////////////

//    public void to_xml(String filename){
//        try {
//            File file = new File(filename);
//            JAXBContext jaxbContext = JAXBContext.newInstance(jaxb.Scenario.class);
//            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
//            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
//            jaxbMarshaller.marshal(to_jaxb(), file);
//        } catch (JAXBException e) {
//            e.printStackTrace();
//        }
//    }

    /** Create a jaxb object for this scenario **/
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
        for(core.Link link : network.links.values()){
            if(link.split_profile!=null){
                for(Map.Entry<Long, SplitMatrixProfile> e : link.split_profile.entrySet()){
                    Long commodity_id = e.getKey();
                    SplitMatrixProfile profile = e.getValue();

                    if(profile==null || profile.get_splits()==null)
                        continue;

                    jaxb.SplitNode jspltnode = new jaxb.SplitNode();
                    jsplits.getSplitNode().add(jspltnode);

                    jspltnode.setCommodityId(commodity_id);
                    if(!Float.isNaN(profile.get_dt()))
                        jspltnode.setDt(profile.get_dt());
                    jspltnode.setStartTime(profile.get_start_time());
                    jspltnode.setLinkIn(profile.get_link_in().getId());
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
    // teminate
    ///////////////////////////////////////////////////

    protected void terminate() {
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

    /** Get a scenario element by type and id **/
    public InterfaceScenarioElement get_element(ScenarioElementType type, Long id){

        switch(type){
            case commodity:
                return commodities.get(id);
            case node:
                return network.nodes.get(id);
            case link:
                return network.links.get(id);
            case roadconnection:
                return network.road_connections.get(id);
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

    ///////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////

    // get maps (needed for py4j to work)

    /** Get network **/
    public Network network(){
        return network;
    }

    /** Get map of models **/
    public Map<String,AbstractModel> models(){
        return models;
    }

    /** Get map of commodities**/
    public Map<Long,Commodity> commodities(){
        return commodities;
    }

    /** Get map of subnetworks **/
    public Map<Long,Subnetwork> subnetworks(){
        return subnetworks;
    }

    /** Get map of controllers  **/
    public Map<Long, AbstractController> controllers(){
        return controllers;
    }

    /** Get map of actuators **/
    public Map<Long, AbstractActuator> actuators(){
        return actuators;
    }

    /** Get map of sensors **/
    public Map<Long, AbstractSensor> sensors(){
        return sensors;
    }

    /** Get map of demands **/
    public Map<Long, Set<DemandInfo>> demands(){
        return demands;
    }

    /** Get map of events **/
    public Map<Long, AbstractScenarioEvent> events(){
        return events;
    }

    // id sets .......................................

    /** Get commodity ids **/
    public Set<Long> commodity_ids(){
        return commodities.keySet();
    }

    /** Get subnetwork ids **/
    public Set<Long> subnetwork_ids(){
        return subnetworks.keySet();
    }

    /** Get ids of subnetworks that are paths **/
    public Set<Long> path_ids(){
        return subnetworks.values().stream()
                .filter(x->x instanceof Path)
                .map(x->x.getId())
                .collect(toSet());
    }

    /** Get actuator ids **/
    public Collection<Long> actuator_ids(){
        return actuators.keySet();
    }

    /** Get sensor ids **/
    public Collection<Long> sensor_ids(){
        return sensors.keySet();
    }

    /** Get controller ids **/
    public Collection<Long> controller_ids(){
        return controllers.keySet();
    }

    // elements by id .................................

    /** Get model by id **/
    public AbstractModel get_model(long id) throws OTMException {
        if(!models.containsKey(id))
            throw new OTMException("Bad id in Scenario.get_model");
        return models.get(id);
    }

    /** Get commodity by id **/
    public Commodity get_commodity(long id) throws OTMException {
        if(!commodities.containsKey(id))
            throw new OTMException("Bad id in Scenario.get_commodity");
        return commodities.get(id);
    }

    /** Get subnetwork by id **/
    public Subnetwork get_subnetwork(long id) throws OTMException {
        if(!subnetworks.containsKey(id))
            throw new OTMException("Bad id in Scenario.get_subnetwork");
        return subnetworks.get(id);
    }

    /** Get actuator by id **/
    public AbstractActuator get_actuator(long id) throws OTMException {
        if(!actuators.containsKey(id))
            throw new OTMException("Bad id in Scenario.get_actuator");
        return actuators.get(id);
    }

    /** Get sensor by id **/
    public AbstractSensor get_sensor(long id) throws OTMException {
        if(!sensors.containsKey(id))
            throw new OTMException("Bad id in Scenario.get_sensor");
        return sensors.get(id);
    }

    /** Get controller by id **/
    public AbstractController get_controller(long id) throws OTMException {
        if(!controllers.containsKey(id))
            throw new OTMException("Bad id in Scenario.get_controller");
        return controllers.get(id);
    }

    // set model .................................

    /**
     * Assign a model to a set of links.
     * @param jmodel Model in jaxb format. Please refer to otm.xsd for the details.
     * @throws OTMException
     */
    public void set_model(jaxb.Model jmodel) throws OTMException {

        if( models.containsKey(jmodel.getName()) )
            throw new OTMException("Duplicate model name in set_model");

        // create the new model. This will remove each link of the new model from its old model
        AbstractModel newmodel = ScenarioFactory.create_model(this,jmodel);

        // remove any orphan models
        models.values().removeIf(model -> model.links.isEmpty());

        // store the new model
        models.put(jmodel.getName(), newmodel);
    }

    // demands .................................

    /** Extract all of the demands for a given commodity.
     * @param commodity_id Commodity id.
     * @return
     */
    public Set<Profile1D> get_demands_for_commodity(Long commodity_id){
        return network.links.values().stream()
                .filter(link->link.demandGenerators!=null)
                .flatMap(link->link.demandGenerators.stream())
                .filter(gen->commodity_id==gen.commodity.getId())
                .map(s->s.profile)
                .collect(toSet());
    }

    /** Extract all of the demands for a given link.
     * @param link_id Link id.
     * @return
     */
    public Set<Profile1D> get_demands_for_link(Long link_id){
        Link link = this.network.links.get(link_id);
        if(link==null)
            return null;
        return link.demandGenerators.stream().map(z->z.profile).collect(toSet());
    }

    // other .................................

    public long add_subnetwork(String name, Set<Long> linkids,Set<Long> comm_ids) throws OTMException {
        Long subnetid = subnetworks.keySet().stream().max(Long::compare).get() + 1;
        Subnetwork newsubnet = new Subnetwork(subnetid,name,linkids,comm_ids,this);
        subnetworks.put(subnetid,newsubnet);
        return subnetid;
    }

    ////////////////////////////////////////////////////////
    // STATE getters and setters -- may be model specific
    ////////////////////////////////////////////////////////

//    public static class Queues {
//        int waiting, transit;
//        public Queues(int waiting, int transit){
//            this.waiting = waiting;
//            this.transit = transit;
//        }
//        public int waiting(){ return waiting ;}
//        public int transit(){ return transit ;}
//    }
//
//    public Queues get_link_queues(long link_id) throws Exception {
//        Link link = network.links.get(link_id);
//        MesoLaneGroup lg = (MesoLaneGroup) link.get_lgs().iterator().next();
//        return new Queues(lg.waiting_queue.num_vehicles(),lg.transit_queue.num_vehicles());
//    }

    /**
     * Integrate the demands to obtain the total number of trips that will take place.
     * @return The number of trips.
     */
    public double get_total_trips() {
        return network.links.values().stream()
                .filter(link->link.has_demands())
                .flatMap(link->link.get_demandGenerators().stream())
                .map(gen->gen.get_total_trips())
                .reduce(0.0,Double::sum);
    }

    ////////////////////////////////////////////////////////
    // animation info
    ////////////////////////////////////////////////////////

    /**
     *
     * @param link_ids Undocumented
     * @return Undocumented
     * @throws OTMException Undocumented
     */
    public AnimationInfo get_animation_info(List<Long> link_ids) throws OTMException {
        return new AnimationInfo(this,link_ids);
    }

    /**
     * Undocumented
     * @return Undocumented
     * @throws OTMException Undocumented
     */
    public AnimationInfo get_animation_info() throws OTMException {
        return new AnimationInfo(this);
    }

}
