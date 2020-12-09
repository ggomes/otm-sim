package api;

import commodity.Subnetwork;
import core.AbstractLaneGroup;
import core.Link;
import core.RoadConnection;
import dispatch.Dispatcher;
import dispatch.EventCreateVehicle;
import dispatch.EventDemandChange;
import dispatch.EventStopSimulation;
import error.OTMException;
import jaxb.OutputRequests;
import models.AbstractModel;
import models.vehicle.spatialq.EventTransitToWaiting;
import models.vehicle.spatialq.MesoLaneGroup;
import models.vehicle.spatialq.MesoVehicle;
import output.*;
import output.animation.AnimationInfo;
import profiles.SplitMatrixProfile;
import cmd.RunParameters;
import core.ScenarioFactory;
import utils.OTMUtils;
import xml.JaxbLoader;
import xml.JaxbWriter;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static java.util.stream.Collectors.toSet;

/**
 * Public API. The methods in the API are of three types. Basic scenario loading and running
 * methods belong to the OTM class. Methods for querying and modifying a scenario belong to
 * api.Scenario. Methods for requesting outputs and calculating different metrics are in api.Output.
 */
public class OTM {

    public core.Scenario scenario;
    public api.Output output;

    ////////////////////////////////////////////////////////
    // construction
    ////////////////////////////////////////////////////////

    public OTM(){}

    /**
     * Constructor. Equivalent to OTM(configfile,true,false)
     * @param configfile Configuration file.
     * @throws OTMException Undocumented
     */
    public OTM(String configfile) throws OTMException {
        load(configfile,true,false);
    }

    /**
     * Constructor.
     * @param configfile Configuration file.
     * @param validate Validate if true.
     * @param jaxb_only Load raw jaxb only if true (ie don't build OTM objects).
     * @throws OTMException Undocumented
     */
    public OTM(String configfile, boolean validate, boolean jaxb_only) throws OTMException {
        load(configfile,validate,jaxb_only);
    }

    ////////////////////////////////////////////////////////
    // load / save
    ////////////////////////////////////////////////////////

    public void load(String configfile, boolean validate, boolean jaxb_only) throws OTMException {
        jaxb.Scenario jaxb_scenario = JaxbLoader.load_scenario(configfile,validate);
        this.scenario =  ScenarioFactory.create_scenario(jaxb_scenario,validate,jaxb_only);
        output = new api.Output(this);
    }

    public void load_from_jaxb(jaxb.Scenario jscn,boolean validate) throws OTMException {
        this.scenario =  ScenarioFactory.create_scenario(jscn,validate,false);
        output = new Output(this);
    }

    public void load_test(String configname) throws OTMException  {
        jaxb.Scenario jaxb_scenario =  JaxbLoader.load_test_scenario(configname+".xml",true);
        this.scenario =  ScenarioFactory.create_scenario(jaxb_scenario,true,false);
        output = new api.Output(this);
    }

    public void save(String file)  {
        try {
            JaxbWriter.save_scenario(scenario.to_jaxb(),file);
        } catch (OTMException e) {
            System.err.println("ERROR");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    ////////////////////////////////////////////////////////
    // initialize
    ////////////////////////////////////////////////////////

    /**
     *  Validate and initialize all components of the scenario. This function must be called prior to calling "advance".
     * @param start_time Initial time in seconds.
     * @throws OTMException Undocumented
     */
    public void initialize(float start_time) throws OTMException {
        initialize(start_time,null,null,null);
    }

    /**
     *  Validate and initialize all components of the scenario. This function must be called prior to calling "advance".
     * @param start_time Initial time in seconds.
     * @param output_requests_file Absolute location and name of file with output requests.
     * @param prefix Prefix for the output.
     * @param output_folder Folder for the output.
     * @throws OTMException Undocumented
     */
    public void initialize(float start_time,String output_requests_file,String prefix,String output_folder) throws OTMException {

        Dispatcher dispatcher = scenario.dispatcher;

        // build and attach dispatcher
        dispatcher = new Dispatcher();

        // append outputs from output request file ..................
        if(output_requests_file!=null && !output_requests_file.isEmpty()) {
            jaxb.OutputRequests jaxb_or = load_output_request(output_requests_file, true);
            scenario.outputs.addAll(create_outputs_from_jaxb(scenario,prefix,output_folder, jaxb_or));
        }

        // initialize
        RunParameters runParams = new RunParameters(prefix,output_requests_file,output_folder,start_time);
        scenario.initialize(dispatcher,runParams);
    }

    ////////////////////////////////////////////////////////
    // run
    ////////////////////////////////////////////////////////

    /**
     * Run the simulation.
     * @param start_time Initial time in seconds.
     * @param duration Duration of the simulation in seconds.
     * @throws OTMException Undocumented
     */
    public void run(float start_time,float duration) throws OTMException {
        initialize(start_time);
        advance(start_time + duration);
        terminate();
        scenario.is_initialized = false;
    }

    /**
     * Run the simulation.
     * @param prefix Prefix for the output.
     * @param output_requests_file Absolute location and name of file with output requests.
     * @param output_folder Folder for the output.
     * @param start_time Initial time in seconds.
     * @param duration Duration of the simulation in seconds.
     * @throws OTMException Undocumented
     */
    public void run(String prefix,String output_requests_file,String output_folder,float start_time,float duration) throws OTMException {
        initialize(start_time,output_requests_file,prefix,output_folder);
        advance(duration);
        terminate();
        scenario.is_initialized = false;
    }

    /**
     *  Advance the simulation in time.
     * @param duration Seconds to advance.
     * @throws OTMException Undocumented
     */
    public void advance(float duration) throws OTMException {

        Dispatcher dispatcher = scenario.dispatcher;

        dispatcher.set_continue_simulation(true);

        // register stop the simulation
        float now = dispatcher.current_time;
        dispatcher.set_stop_time(now+duration);
        dispatcher.register_event(new EventStopSimulation(scenario,dispatcher,now+duration));

        // process all events
        dispatcher.dispatch_events_to_stop();
    }

    public void terminate() {
        scenario.terminate();
    }

    ////////////////////////////////////////////////////////
    // getters
    ////////////////////////////////////////////////////////

    /**
     * Get set of all path ids (ie linear subnetworks that begin at a source)
     * @return Set of path ids
     */
    public Set<Long> get_path_ids(){
        return scenario.subnetworks.values().stream()
                .filter(x->x.isPath())
                .map(x->x.getId())
                .collect(toSet());
    }

    public long add_subnetwork(String name, Set<Long> linkids,Set<Long> comm_ids) throws OTMException {
        Long subnetid = scenario.subnetworks.keySet().stream().max(Long::compare).get() + 1;
        Subnetwork newsubnet = new Subnetwork(subnetid,name,linkids,comm_ids,scenario);
        scenario.subnetworks.put(subnetid,newsubnet);
        return subnetid;
    }

    public boolean remove_subnetwork(long subnetid) {
        if(!scenario.subnetworks.containsKey(subnetid))
            return false;
        scenario.subnetworks.remove(subnetid);
        return true;
    }

    public void subnetwork_remove_links(long subnetid,Set<Long> linkids) throws OTMException {
        if(!scenario.subnetworks.containsKey(subnetid))
            throw new OTMException("Bad subnetwork id in subnetwork_delete_link");

        if(!scenario.network.links.keySet().containsAll(linkids))
            throw new OTMException("Bad link id in subnetwork_delete_link");

        Set<Link> links = linkids.stream().map(i->scenario.network.links.get(i)).collect(toSet());
        scenario.subnetworks.get(subnetid).remove_links(links);
    }

    public void subnetwork_add_links(long subnetid,Set<Long> linkids) throws OTMException {
        if(!scenario.subnetworks.containsKey(subnetid))
            throw new OTMException("Bad subnetwork id in subnetwork_add_link");

        if(!scenario.network.links.keySet().containsAll(linkids))
            throw new OTMException("Bad link id in subnetwork_add_link");

        Set<Link> links = linkids.stream().map(i->scenario.network.links.get(i)).collect(toSet());
        scenario.subnetworks.get(subnetid).add_links(links);
    }

    ////////////////////////////////////////////////////////
    // network
    ////////////////////////////////////////////////////////

    /**
     * Returns a set where every entry is a list with entries [link_id,start_node,end_node]
     * @return A set of lists
     */
    public Set<List<Long>> get_link_connectivity(){
        Set<List<Long>> X = new HashSet<>();
        for(Link link : scenario.network.links.values()){
            List<Long> A = new ArrayList<>();
            A.add(link.getId());
            A.add(link.start_node.getId());
            A.add(link.end_node.getId());
            X.add(A);
        }
        return X;
    }

    /**
     * Get ids for all source links.
     * @return A set of ids.
     */
    public Set<Long> get_source_link_ids(){
        return scenario.network.links.values().stream()
                .filter(x->x.is_source)
                .map(x->x.getId())
                .collect(toSet());
    }

    /**
     * Get the lane groups that enter a given road connection
     * @param rcid Id of the road connection
     * @return A set of lane group ids.
     */
    public Set<Long> get_in_lanegroups_for_road_connection(long rcid){
        RoadConnection rc = scenario.network.get_road_connection(rcid);
        Set<Long> lgids = new HashSet<>();
        for(AbstractLaneGroup lg : rc.in_lanegroups)
            lgids.add(lg.id);
        return lgids;
    }

    /**
     * Get the lane groups that exit a given road connection
     * @param rcid Id of the road connection
     * @return A set of lane group ids.
     */
    public Set<Long> get_out_lanegroups_for_road_connection(long rcid){
        RoadConnection rc = scenario.network.get_road_connection(rcid);
        Set<Long> lgids = new HashSet<>();
        for(AbstractLaneGroup lg : rc.out_lanegroups)
            lgids.add(lg.id);
        return lgids;
    }

    /**
     * Get lane groups in every link
     * @return A map from link ids to a set of lane group ids.
     */
    public Map<Long,Set<Long>> get_link2lgs(){
        Map<Long,Set<Long>> lk2lgs = new HashMap<>();
        for(Link link : scenario.network.links.values())
            lk2lgs.put(link.getId(),link.lanegroups_flwdn.stream()
                    .map(x->x.id).collect(toSet()));
        return lk2lgs;
    }

    ////////////////////////////////////////////////////////
    // STATE getters and setters -- may be model specific
    ////////////////////////////////////////////////////////

    public static class Queues {
        int waiting, transit;
        public Queues(int waiting, int transit){
            this.waiting = waiting;
            this.transit = transit;
        }
        public int waiting(){ return waiting ;}
        public int transit(){ return transit ;}
    }

    public Queues get_link_queues(long link_id) throws Exception {
        Link link = scenario.network.links.get(link_id);
        MesoLaneGroup lg = (MesoLaneGroup) link.lanegroups_flwdn.iterator().next();
        return new Queues(lg.waiting_queue.num_vehicles(),lg.transit_queue.num_vehicles());
    }

    /** Set the number of vehicles in a link
     * This only works for a single commodity scenarios, and single lane group links.
     * @param link_id
     * @param numvehs_waiting
     * @param numvehs_transit
     */
    public void set_link_vehicles(long link_id, int numvehs_waiting,int numvehs_transit) throws Exception {

        if(scenario.commodities.size()>1)
            throw new Exception("Cannot call set_link_vehicles on multi-commodity networks");

        if(!scenario.network.links.containsKey(link_id))
            throw new Exception("Bad link id");

        Link link = scenario.network.links.get(link_id);

        if(link.lanegroups_flwdn.size()>1)
            throw new Exception("Cannot call set_link_vehicles on multi-lane group links");

//        if(link.model.type!= ModelType.VehicleMeso)
//            throw new Exception("Cannot call set_link_vehicles on non-meso models");

        long comm_id = scenario.commodities.keySet().iterator().next();
        MesoLaneGroup lg = (MesoLaneGroup) link.lanegroups_flwdn.iterator().next();
        SplitMatrixProfile smp = lg.link.split_profile.get(comm_id);

        // transit queue ................
        models.vehicle.spatialq.Queue tq = lg.transit_queue;
        tq.clear();
        for(int i=0;i<numvehs_transit;i++) {
            MesoVehicle vehicle = new MesoVehicle(comm_id, null);

            // sample the split ratio to decide where the vehicle will go
            Long next_link_id = smp.sample_output_link();
            vehicle.set_next_link_id(next_link_id);

            // set the vehicle's lane group and state
            vehicle.lg = lg;
            vehicle.my_queue = tq;

            // add to lane group (as in lg.add_vehicle_packet)
            tq.add_vehicle(vehicle);

            // register_with_dispatcher dispatch to go to waiting queue
            Dispatcher dispatcher = scenario.dispatcher;
            float timestamp = scenario.get_current_time();
            float transit_time_sec = (float) OTMUtils.random_zero_to_one()*lg.transit_time_sec;
            dispatcher.register_event(new EventTransitToWaiting(dispatcher,timestamp + transit_time_sec,vehicle));
        }

        // waiting queue .................
        models.vehicle.spatialq.Queue wq = lg.waiting_queue;
        wq.clear();
        for(int i=0;i<numvehs_waiting;i++) {
            MesoVehicle vehicle = new MesoVehicle(comm_id, null);

            // sample the split ratio to decide where the vehicle will go
            Long next_link_id = smp.sample_output_link();
            vehicle.set_next_link_id(next_link_id);

            // set the vehicle's lane group and key
            vehicle.lg = lg;
            vehicle.my_queue = wq;

            // add to lane group (as in lg.add_vehicle_packet)
            wq.add_vehicle(vehicle);
        }

    }

    /**
     *  Clear all demands in the scenario.
     */
    public void clear_all_demands(){

        if(scenario ==null)
            return;

        // delete sources from links
        for(Link link : scenario.network.links.values()) {
            if (link.demandGenerators == null || link.demandGenerators.isEmpty())
                continue;
            link.demandGenerators.clear();
        }

        // delete all EventCreateVehicle and EventDemandChange from dispatcher
        if(scenario.dispatcher!=null) {
            scenario.dispatcher.remove_events_of_type(EventCreateVehicle.class);
            scenario.dispatcher.remove_events_of_type(EventDemandChange.class);
        }

    }

    /**
     * Integrate the demands to obtain the total number of trips that will take place.
     * @return The number of trips.
     */
    public double get_total_trips() {
        return scenario.network.links.values().stream()
                .filter(link->link.demandGenerators !=null && !link.demandGenerators.isEmpty())
                .flatMap(link->link.demandGenerators.stream())
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
        return new AnimationInfo(scenario,link_ids);
    }

    /**
     * Undocumented
     * @return Undocumented
     * @throws OTMException Undocumented
     */
    public AnimationInfo get_animation_info() throws OTMException {
        return new AnimationInfo(scenario);
    }


    ////////////////////////////////////////////////////////
    // getters
    ////////////////////////////////////////////////////////

    public Output output(){
        return output;
    }

    /**
     * Current simulation time in seconds.
     * @return Current simulation time in seconds.
     */
    public float get_current_time(){
        return scenario.get_current_time();
    }

    ////////////////////////////////////////////////////////
    // static
    ////////////////////////////////////////////////////////

    /**
     * Git hash for the current build.
     * @return Git hash for the current build.
     */
    public static String get_version(){
        InputStream inputStream = cmd.OTM.class.getResourceAsStream("/otm-sim.properties");
        Properties properties = new Properties();
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read properties file", e);
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        return properties.getProperty("sim.git");
    }

    /**
     * Set the seed for the simulator's random number generator.
     * @param seed Any number
     */
    public static void set_random_seed(long seed){
        OTMUtils.set_random_seed(seed);
    }

    ////////////////////////////////////////////////////////
    // private
    ////////////////////////////////////////////////////////

    private static jaxb.OutputRequests load_output_request(String filename, boolean validate) throws OTMException {
        try {

            JAXBContext jaxbContext = JAXBContext.newInstance(OutputRequests.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            if(validate) {
                SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                InputStream resourceAsStream = JaxbLoader.class.getResourceAsStream("/outputs.xsd");
                Schema schema = sf.newSchema(new StreamSource(resourceAsStream));
                unmarshaller.setSchema(schema);
            }

            OutputRequests jaxb_outputrequests = (OutputRequests) unmarshaller.unmarshal(new File(filename));
            return jaxb_outputrequests;
        } catch(org.xml.sax.SAXException e){
            throw new OTMException(e);
        }  catch (JAXBException e) {
            throw new OTMException(e);
        }
    }

    private static Set<AbstractOutput> create_outputs_from_jaxb(core.Scenario scenario, String prefix, String output_folder, jaxb.OutputRequests jaxb_ors) throws OTMException{
        Set<AbstractOutput> outputs = new HashSet<>();
        if(jaxb_ors==null)
            return outputs;
        AbstractOutput output;
        for(jaxb.OutputRequest jaxb_or : jaxb_ors.getOutputRequest()){

            Long commodity_id = jaxb_or.getCommodity();
            Float outDt = jaxb_or.getDt();

            if(jaxb_or.getModel()!=null){
                if(!scenario.network.models.containsKey(jaxb_or.getModel()))
                    throw new OTMException("Bad model name in output : " + jaxb_or.getModel());
                AbstractModel model = scenario.network.models.get(jaxb_or.getModel());
                output = model.create_output_object(scenario,prefix,output_folder,jaxb_or);
            }

            else {

                switch (jaxb_or.getQuantity()) {
                    case "lanegroups":
                        output = new OutputLaneGroups(scenario, prefix, output_folder);
                        break;
                    case "link_flw":
                        output = new OutputLinkFlow(scenario, prefix, output_folder, commodity_id, null, outDt);
                        break;
                    case "link_veh":
                        output = new OutputLinkVehicles(scenario, prefix, output_folder, commodity_id, null, outDt);
                        break;
                    case "lanegroup_flw":
                        output = new OutputLaneGroupFlow(scenario, prefix, output_folder, commodity_id, null, outDt);
                        break;
                    case "lanegroup_veh":
                        output = new OutputLaneGroupVehicles(scenario, prefix, output_folder, commodity_id, null, outDt);
                        break;
                    case "link_vht":
                        output = new OutputLinkVHT(scenario, prefix, output_folder, commodity_id, null, outDt);
                        break;
                    case "vehicle_events":
                        output = new OutputVehicle(scenario, prefix, output_folder, commodity_id);
                        break;
                    case "vehicle_class":
                        output = new OutputVehicleClass(scenario, prefix, output_folder);
                        break;
                    case "vehicle_travel_time":
                        output = new OutputTravelTime(scenario, prefix, output_folder);
                        break;
                    case "controller":
                        output = new OutputController(scenario, prefix, output_folder, jaxb_or.getController());
                        break;
//                    case "actuator":
//                        output = new OutputActuator(scenario, prefix, output_folder, jaxb_or.getActuator());
//                        break;
                    case "path_travel_time":
                        output = new OutputPathTravelTime(scenario, prefix, output_folder, null, outDt);
                        scenario.path_tt_manager.add_path_travel_time_writer((OutputPathTravelTime) output);
                        break;
                    default:
                        throw new OTMException("Bad output identifier : " + jaxb_or.getQuantity());
                }

            }

            if(output!=null)
                outputs.add(output);
        }
        return outputs;
    }
}