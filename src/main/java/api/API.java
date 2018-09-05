package api;

import actuator.AbstractActuator;
import actuator.sigint.ActuatorSignal;
import api.info.*;
import commodity.Commodity;
import commodity.Subnetwork;
import common.AbstractLaneGroup;
import common.RoadConnection;
import control.AbstractController;
import dispatch.EventCreateVehicle;
import dispatch.EventDemandChange;
import error.OTMErrorLog;
import error.OTMException;
import common.Link;
import keys.DemandType;
import keys.KeyCommodityDemandTypeId;
import output.*;
import output.animation.AnimationInfo;
import profiles.AbstractDemandProfile;
import profiles.DemandProfile;
import runner.OTM;
import runner.Scenario;
import runner.ScenarioFactory;
import sensor.AbstractSensor;
import utils.OTMUtils;
import utils.StochasticProcess;
import xml.JaxbLoader;

import java.util.*;


import static java.util.stream.Collectors.toList;

public class API {

    protected Scenario scenario;

    public API(){
        this.scenario = null;
    }

    public String get_version(){
        return OTM.getGitHash();
    }

    public void load(String configfile) throws OTMException{
        load(configfile,Float.NaN,true,null);
    }

    public void load(String configfile,float sim_dt) throws OTMException{
        load(configfile,sim_dt,true,null);
    }

    public List<Long> load(String configfile,float sim_dt,boolean validate) throws OTMException{
        return load(configfile,sim_dt,validate,null);
    }

    public List<Long> load_for_static_traffic_assignment(String configfile) throws OTMException{

        List<Long> timestamps = new ArrayList<>();
        Date now = new Date();

        timestamps.add(now.getTime());
        jaxb.Scenario jaxb_scenario = JaxbLoader.load_scenario(configfile,false);
        now = new Date();
        timestamps.add(now.getTime());
        System.out.println("Took " + (timestamps.get(1)-timestamps.get(0)) + " to load XML.");

        this.scenario =  ScenarioFactory.create_scenario_for_static_traffic_assignment(jaxb_scenario);
//        this.scenario =  ScenarioFactory.create_scenario(jaxb_scenario,1f,true,"ctm");

        now = new Date();
        timestamps.add(now.getTime());
        System.out.println("Took " + (timestamps.get(2)-timestamps.get(1)) + " to create scenario.");

        return timestamps;

    }

    public List<Long> load(String configfile,float sim_dt,boolean validate, String global_model) throws OTMException{

        List<Long> timestamps = new ArrayList<>();
        Date now = new Date();

        timestamps.add(now.getTime());
        jaxb.Scenario jaxb_scenario = JaxbLoader.load_scenario(configfile,validate);
        now = new Date();
        timestamps.add(now.getTime());

        this.scenario =  ScenarioFactory.create_scenario(jaxb_scenario,sim_dt,validate,global_model);

        now = new Date();
        timestamps.add(now.getTime());

        return timestamps;

    }

    public List<Long> load_test(String testname,float sim_dt,boolean validate, String global_model) throws OTMException{

        List<Long> timestamps = new ArrayList<>();
        Date now = new Date();

        timestamps.add(now.getTime());
        jaxb.Scenario jaxb_scenario = JaxbLoader.load_test_scenario(testname,validate);
        now = new Date();
        timestamps.add(now.getTime());

        this.scenario =  ScenarioFactory.create_scenario(jaxb_scenario,sim_dt,validate,global_model);

        now = new Date();
        timestamps.add(now.getTime());

        return timestamps;

    }

    public void set_random_seed(long seed){
        OTMUtils.set_random_seed(seed);
    }

    ////////////////////////////////////////////////////////
    // scenario
    ////////////////////////////////////////////////////////

    public boolean has_scenario(){
        return scenario!=null;
    }

    /**
     * Get scenario information.
     * <p>
     * Returns a ScenarioInfo object, containing all of the input
     * data for this scenario.
     * </p>
     *
     * @return a ScenarioInfo object.
     * @see ScenarioInfo
     */
    public ScenarioInfo get_scenario_info(){
        return scenario!=null ? new ScenarioInfo(scenario) : null;
    }

    /**
     * Sets the type of process governing the creation and release of vehicles.
     * @param str in {"poisson","deterministic"}
     */
    public void set_stochastic_process(String str){
        scenario.set_stochastic_process(StochasticProcess.valueOf(str));
    }

    ////////////////////////////////////////////////////////
    // commodities
    ////////////////////////////////////////////////////////

    /**
     * Get the total number of commodities in the scenario.
     *
     * @return an integer.
     */
    public int get_num_commodities(){
        return scenario.commodities.size();
    }

    /**
     * Get information for all commodities in the scenario.
     *
     * @return a list of CommodityInfo
     * @see CommodityInfo
     */
    public List<CommodityInfo> get_commodities(){
        List<CommodityInfo> commInfo = new ArrayList<>();
        for(Commodity comm : scenario.commodities.values())
            commInfo.add(new CommodityInfo(comm));
        return commInfo;
    }

    /**
     * Get information for a specific commodity.
     *
     * @param  id Integer commodity id
     * @return A CommodityInfo object
     * @see CommodityInfo
     */
    public CommodityInfo get_commodity_with_id(long id){
        Commodity comm = scenario.commodities.get(id);
        return comm==null? null : new CommodityInfo(comm);
    }

    public List<Long> get_commodity_ids(){
        return new ArrayList(scenario.commodities.keySet());
    }

    ////////////////////////////////////////////////////////
    // subnetworks and paths
    ////////////////////////////////////////////////////////

    /**
     * Get the total number of subnetworks in the scenario.
     *
     * @return an integer.
     */
    public int get_num_subnetworks(){
        return scenario.subnetworks.size();
    }

    /**
     * Get list of all subnetwork ids
     */
    public List<Long> get_subnetwork_ids(){
        return new ArrayList(scenario.subnetworks.keySet());
    }

    /**
     * Get list of all path ids (ie linear subnetworks that begin at a source)
     */
    public List<Long> get_path_ids(){
        return scenario.subnetworks.values().stream()
                .filter(x->x.is_path)
                .map(x->x.getId())
                .collect(toList());
    }

    /**
     * Get information for all subnetworks in the scenario.
     *
     * @return a list of SubnetworkInfo
     * @see SubnetworkInfo
     */
    public List<SubnetworkInfo> get_subnetworks(){
        List<SubnetworkInfo> subnetInfo = new ArrayList<>();
        for(Subnetwork subnet : scenario.subnetworks.values())
            subnetInfo.add(new SubnetworkInfo(subnet));
        return subnetInfo;
    }

    /**
     * Get information for a specific subnetwork.
     *
     * @param  id Integer subnetwork id
     * @return A SubnetworkInfo object
     * @see SubnetworkInfo
     */
    public SubnetworkInfo get_subnetwork_with_id(long id){
        Subnetwork subnet = scenario.subnetworks.get(id);
        return subnet==null? null : new SubnetworkInfo(subnet);
    }

    ////////////////////////////////////////////////////////
    // network
    ////////////////////////////////////////////////////////

    /**
     * Get the total number of links in the scenario.
     *
     * @return an integer.
     */
    public int get_num_links(){
        return scenario.network.links.size();
    }

    /**
     * Get the total number of nodes in the scenario.
     *
     * @return an integer.
     */
    public int get_num_nodes(){
        return scenario.network.nodes.size();
    }

    /**
     * Get list of all node ids
     */
    public List<Long> get_node_ids(){
        return new ArrayList(scenario.network.nodes.keySet());
    }

    /**
     * Returns a list where every entry is a list with entries [link_id,start_node,end_node]
     */
    public List<List<Long>> get_link_connectivity(){
        List<List<Long>> X = new ArrayList<>();
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
     * Get information for all links in the scenario.
     *
     * @return a list of LinkInfo
     * @see LinkInfo
     */
    public List<LinkInfo> get_links(){
        List<LinkInfo> linkInfo = new ArrayList<>();
        for(Link link : scenario.network.links.values())
            linkInfo.add(new LinkInfo(link));
        return linkInfo;
    }

    /**
     * Get information for a specific link.
     *
     * @param  id Integer link id
     * @return A LinkInfo object
     * @see LinkInfo
     */
    public LinkInfo get_link_with_id(long id){
        Link link = scenario.network.links.get(id);
        return link==null ? null : new LinkInfo(link);
    }

    /**
     * Get the list of ids of all links in the network.
     * @return A List of longs.
     */
    public List<Long> get_link_ids(){
        return new ArrayList(scenario.network.links.keySet());
    }

    /**
     * Get ids for all source links.
     *
     * @return List of integer ids
     */
    public List<Long> get_source_link_ids(){
        return scenario.network.links.values().stream()
                .filter(x->x.is_source)
                .map(x->x.getId())
                .collect(toList());
    }

    public List<Long> get_in_lanegroups_for_road_connection(long rcid){
        RoadConnection rc = scenario.network.get_road_connection(rcid);
        List<Long> lgids = new ArrayList<>();
        for(AbstractLaneGroup lg : rc.in_lanegroups)
            lgids.add(lg.id);
        return lgids;
    }

    public List<Long> get_out_lanegroups_for_road_connection(long rcid){
        RoadConnection rc = scenario.network.get_road_connection(rcid);
        List<Long> lgids = new ArrayList<>();
        for(AbstractLaneGroup lg : rc.out_lanegroups)
            lgids.add(lg.id);
        return lgids;
    }

    ////////////////////////////////////////////////////////
    // demands / splits
    ////////////////////////////////////////////////////////

    /**
     * Get information for all demands in the scenario.
     *
     * @return a list of DemandInfo
     * @see DemandInfo
     */
    public List<DemandInfo> get_demands(){
        List<DemandInfo> x = new ArrayList<>();
        for(AbstractDemandProfile y : scenario.data_demands.values())
            x.add(new DemandInfo(y));
        return x;
    }

    /**
     * Get information for a specific demand.
     *
     * @param typestr : [String] "pathless" or "pathfull".
     * @param link_or_path_id : [long] integer id of the source link if pathless, or of the subnetwork if pathfull.
     * @param commodity_id " [long] integer id of the commodity.
     * @return A DemandInfo object
     * @see DemandInfo
     */
    public DemandInfo get_demand_with_ids(String typestr,long link_or_path_id,long commodity_id){
        DemandType type = DemandType.valueOf(typestr);
        if(type==null)
            return null;
        AbstractDemandProfile dp = scenario.data_demands.get(new KeyCommodityDemandTypeId(commodity_id,link_or_path_id,type));
        return dp==null ? null : new DemandInfo(dp);
    }

    public void clear_all_demands(){

        if(scenario==null)
            return;

        // delete sources from links
        for(Link link : scenario.network.links.values())
            link.sources = new HashSet<>();

        // delete all EventCreateVehicle and EventDemandChange from dispatcher
        if(scenario.dispatcher!=null) {
            scenario.dispatcher.remove_events_for_recipient(EventCreateVehicle.class);
            scenario.dispatcher.remove_events_for_recipient(EventDemandChange.class);
        }

        // delete all demand profiles
        if(scenario.data_demands!=null)
            scenario.data_demands.clear();
    }

    /**
     * Set or override a demand value for a path.
     * <p>
     * Use this method to set a demand profile of a given commodity on a given path.
     * The profile is a piecewise continuous function starting a time "start_time" and with
     * sample time "dt". The values are given by the "values" array. The value before
     * before "start_time" is zero, and the last value in the array is held into positive
     * infinity time.
     * </p>
     * This method will override any existing demands for that commodity and path.
     *
     * @param path_id : [long] integer id of the subnetwork
     * @param commodity_id : [long] integer id of the commodity
     * @param start_time : [float] start time for the demand profile in seconds after midnight.
     * @param dt : [float] step time for the profile in seconds.
     * @param values : [array of doubles] list of values for the piecewise continuous profile.
     */
    public void set_demand_on_path_in_vph(long path_id,long commodity_id,float start_time,float dt,List<Double> values) throws OTMException {

        Subnetwork path = scenario.subnetworks.get(path_id);
        if(path==null)
            throw new OTMException("Bad path id");

        Commodity commodity = scenario.commodities.get(commodity_id);
        if(commodity==null)
            throw new OTMException("Bad commodity id");


//        List<Double> valuelist = new ArrayList<>();
//        for(double v : values)
//            valuelist.add(v/3600.0);    // change to vps

        DemandProfile dp = new DemandProfile(path,commodity,start_time, dt, values);

        // validate
        OTMErrorLog errorLog = new OTMErrorLog();
        dp.validate(errorLog);
        if (errorLog.haserror())
            throw new OTMException(errorLog.format_errors());

        // add to scenario
        scenario.data_demands.put(dp.get_key(),dp);

        if(scenario.is_initialized) {
            // initialize
            dp.initialize(scenario);

            // send to dispatcher
            dp.register_initial_events(scenario.dispatcher);
        }

    }

    public List<ODInfo> get_od_info() throws OTMException {

        Map<ODPair,ODInfo> odmap = new HashMap<>();

        for(AbstractDemandProfile demand_profile : scenario.data_demands.values()){

            if(demand_profile.get_type()==DemandType.pathless)
                continue;

            Long origin_node_id = demand_profile.get_origin_node_id();
            Long destination_node_id = demand_profile.get_destination_node_id();
            Long commodity_id = demand_profile.commodity.getId();

            ODPair odpair = new ODPair(origin_node_id,destination_node_id,commodity_id);
            ODInfo odinfo;
            if(odmap.containsKey(odpair)){
                odinfo = odmap.get(odpair);
            } else {
                odinfo = new ODInfo(odpair);
                odmap.put(odpair,odinfo);
            }
            odinfo.add_demand_profile(demand_profile);
        }

        return new ArrayList(odmap.values());
    }

    public double get_total_trips() {
        return scenario.data_demands.values().stream()
                .map(x->x.get_total_trips())
                .reduce(0.0,Double::sum);
    }

    ////////////////////////////////////////////////////////
    // sensors
    ////////////////////////////////////////////////////////

    /**
     * Get the total number of sensors in the scenario.
     *
     * @return an integer.
     */
    public int get_num_sensors(){
        return scenario.sensors.size();
    }

    /**
     * Get information for all sensors in the scenario.
     *
     * @return a list of SensorInfo
     * @see SensorInfo
     */
    public List<SensorInfo> get_sensors(){
        List<SensorInfo> x = new ArrayList<>();
        for(AbstractSensor y : scenario.sensors.values())
            x.add(new SensorInfo(y));
        return x;
    }

    /**
     * Get information for a specific sensor.
     *
     * @param id : [long] integer id of the sensor.
     * @return A SensorInfo object
     * @see SensorInfo
     */
    public SensorInfo get_sensor_with_id(long id){
        AbstractSensor sensor = scenario.sensors.get(id);
        return sensor==null ? null : new SensorInfo(sensor);
    }

    ////////////////////////////////////////////////////////
    // controllers
    ////////////////////////////////////////////////////////

    /**
     * Get the total number of controllers in the scenario.
     *
     * @return an integer.
     */
    public int get_num_controllers(){
        return scenario.controllers.size();
    }

    /**
     * Get information for all controllers in the scenario.
     *
     * @return a list of ControllerInfo
     * @see ControllerInfo
     */
    public List<ControllerInfo> get_controllers(){
        List<ControllerInfo> x = new ArrayList<>();
        for(AbstractController y : scenario.controllers.values())
            x.add(new ControllerInfo(y));
        return x;
    }

    /**
     * Get information for a specific myController.
     *
     * @param id : [long] integer id of the myController.
     * @return A ControllerInfo object
     * @see ControllerInfo
     */
    public ControllerInfo get_controller_with_id(long id){
        AbstractController controller = scenario.controllers.get(id);
        return controller==null ? null : new ControllerInfo(controller);
    }

    ////////////////////////////////////////////////////////
    // actuators
    ////////////////////////////////////////////////////////

    /**
     * Get the total number of actuators in the scenario.
     *
     * @return an integer.
     */
    public int get_num_actuators(){
        return scenario.actuators.size();
    }

    /**
     * Get information for all actuators in the scenario.
     *
     * @return a list of ActuatorInfo
     * @see ActuatorInfo
     */
    public List<ActuatorInfo> get_actuators(){
        List<ActuatorInfo> x = new ArrayList<>();
        for(AbstractActuator y : scenario.actuators.values())
            x.add(create_actuator_info(y));
        return x;
    }

    /**
     * Get information for a specific actuator.
     *
     * @param id : [long] integer id of the actuator.
     * @return A ActuatorInfo object
     * @see ActuatorInfo
     */
    public ActuatorInfo get_actuator_with_id(long id){
        return create_actuator_info( scenario.actuators.get(id) );
    }

    ////////////////////////////////////////////////////////
    // performance
    ////////////////////////////////////////////////////////

//    public PerformanceInfo get_performance(){
//        return get_performance_for_commodity(null);
//    }
//
//    public PerformanceInfo get_performance_for_commodity(Long commodity_id) {
//
//        // get commodity
//        Commodity commodity;
//        Collection<Link> links;
//        if (commodity_id == null){
//            commodity = null;
//            links = scenario.network.links.values();
//        }
//        else{
//            commodity = scenario.commodities.get(commodity_id);
//            if(commodity==null)
//                return null;
//            links = commodity.all_links;
//        }
//        return new PerformanceInfo(commodity,links);
//    }

    ////////////////////////////////////////////////////////
    // run
    ////////////////////////////////////////////////////////

    public void run(String prefix,String output_requests_file,String output_folder,float start_time,float duration) {
        try {
            OTM.run(scenario,prefix,output_requests_file,output_folder,start_time,duration);
        } catch (OTMException e) {
            e.printStackTrace();
        }

//        try {
//            for(FileWriter writer : scenario.writer.values()){
//                writer.close();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    /** Run with no simulation dt. Good for pure models.ctm.pq only **/
    public void run(float start_time,float duration) {
        run(null,null,null,start_time,duration);
    }

    public void run(String runfile) {
        try {
            OTM.run(scenario,runfile);
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    ////////////////////////////////////////////////////////
    // outputs
    ////////////////////////////////////////////////////////

    public Set<AbstractOutput> get_output_data(){
        Set<AbstractOutput> x = new HashSet<>();
        for(AbstractOutput output : scenario.outputs)
            if(!output.write_to_file)
                x.add(output);
        return x;
    }

    public void clear_output_requests(){
        scenario.outputs.clear();
    }

    public List<String> get_outputs(){
        return scenario.outputs.stream().map(x->x.get_output_file()).collect(toList());
    }

    // network ................

    public void request_lanegroups(String prefix,String output_folder){
        try {
            this.scenario.outputs.add(new LaneGroups(scenario,prefix,output_folder));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    // links ....................

    public void request_links_veh(String prefix,String output_folder,Long commodity_id,List<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LinkVehicles(scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    public void request_links_veh(Long commodity_id,List<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LinkVehicles(scenario,null,null,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    public void request_links_flow(String prefix,String output_folder,Long commodity_id,List<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LinkFlow(scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    public void request_links_flow(Long commodity_id,List<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LinkFlow(scenario,null,null,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    // lanegroups ...............

    public void request_lanegroup_flw(String prefix,String output_folder,Long commodity_id,List<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LaneGroupFlow(scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    public void request_lanegroup_flw(Long commodity_id,List<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LaneGroupFlow(scenario,null,null,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    public void request_lanegroup_veh(String prefix,String output_folder,Long commodity_id,List<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LaneGroupVehicles(scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    public void request_lanegroup_veh(Long commodity_id,List<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LaneGroupVehicles(scenario,null,null,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    // subnetwroks ..............

    /**
     * Request the travel times on a given path be recorded
     * @param prefix Prefix for the output file.
     * @param output_folder Prefix for the output file.
     * @param subnetwork_id
     * @param outDt in seconds
     */
    public void request_path_travel_time(String prefix,String output_folder,Long subnetwork_id,Float outDt){
        try {
            this.scenario.outputs.add(new PathTravelTime(scenario,prefix,output_folder,subnetwork_id,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request the travel times on a given path be recorded. Results are held in memory, not written to a file.
     * @param subnetwork_id
     * @param outDt in seconds
     */
    public void request_path_travel_time(Long subnetwork_id,Float outDt){
        request_path_travel_time(null,null,subnetwork_id,new Float(outDt));
    }

    public void request_subnetwork_flw(String prefix,String output_folder,Long commodity_id,Long subnetwork_id,Float outDt){
        try {
            this.scenario.outputs.add(new SubnetworkFlow(scenario,prefix,output_folder,commodity_id,subnetwork_id,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    public void request_subnetwork_veh(String prefix,String output_folder,Long commodity_id,Long subnetwork_id,Float outDt){
        try {
            this.scenario.outputs.add(new SubnetworkVehicles(scenario,prefix,output_folder,commodity_id,subnetwork_id,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    public void request_subnetwork_vht(String prefix,String output_folder,Long commodity_id,Long subnetwork_id,Float outDt){
        try {
            this.scenario.outputs.add(new LinkVHT(scenario,prefix,output_folder,commodity_id,subnetwork_id,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    // vehicles .................

    public void request_vehicle_events(float commodity_id){
        try {
            this.scenario.outputs.add(new OutputEventsVehicle(scenario,null,null,(long) commodity_id));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    public void request_vehicle_events(String prefix,String output_folder,Long commodity_id){
        try {
            this.scenario.outputs.add(new OutputEventsVehicle(scenario,prefix,output_folder,commodity_id));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    public void request_vehicle_class(String prefix,String output_folder){
        this.scenario.outputs.add(new VehicleClass(scenario,prefix,output_folder));
    }

    public void request_vehicle_travel_time(String prefix,String output_folder){
        this.scenario.outputs.add(new VehicleTravelTime(scenario,prefix,output_folder));
    }

    // sensors ..................

    // actuators ...............

    public void request_actuator(String prefix,String output_folder,Long actuator_id){
        try {
            this.scenario.outputs.add(new OutputEventsActuator(scenario,prefix,output_folder,actuator_id));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    public void request_actuator(Long actuator_id){
        request_actuator(null,null,actuator_id);
    }

    // controllers ..............

    public void request_controller(String prefix,String output_folder,Long controller_id){
        try {
            this.scenario.outputs.add(new OutputEventsController(scenario,prefix,output_folder,controller_id));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    public void request_controller(Long controller_id){
        request_controller(null,null, controller_id);
    }

    ////////////////////////////////////////////////////////
    // animation
    ////////////////////////////////////////////////////////

    public void initialize(float start_time) throws OTMException{
        OTM.initialize(scenario,start_time);
    }

    public void advance(float duration) throws OTMException{
        OTM.advance(scenario,duration);
    }

    public float get_current_time(){
        return scenario.get_current_time();
    }

    public AnimationInfo get_animation_info(List<Long> link_ids) throws OTMException {
        return new AnimationInfo(scenario,link_ids);
    }

    public AnimationInfo get_animation_info() throws OTMException {
        return new AnimationInfo(scenario);
    }

    ////////////////////////////////////////////////////////
    // private
    ////////////////////////////////////////////////////////

    private ActuatorInfo create_actuator_info(AbstractActuator actuator){

        if(actuator==null)
            return null;

        if(actuator instanceof ActuatorSignal)
            return new SignalInfo((ActuatorSignal) actuator);

        return new ActuatorInfo(actuator);
    }

}