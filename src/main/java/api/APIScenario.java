package api;

import actuator.AbstractActuator;
import actuator.sigint.ActuatorSignal;
import api.info.*;
import commodity.Commodity;
import commodity.Subnetwork;
import common.Link;
import common.RoadConnection;
import control.AbstractController;
import dispatch.EventCreateVehicle;
import dispatch.EventDemandChange;
import error.OTMErrorLog;
import error.OTMException;
import keys.DemandType;
import keys.KeyCommodityDemandTypeId;
import models.AbstractLaneGroup;
import output.animation.AnimationInfo;
import profiles.AbstractDemandProfile;
import profiles.DemandProfile;
import runner.Scenario;
import sensor.AbstractSensor;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class APIScenario {

    private Scenario scenario;
    protected APIScenario(Scenario scenario){
        this.scenario = scenario;
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
    public ScenarioInfo get_info(){
        return scenario !=null ? new ScenarioInfo(scenario) : null;
    }

    ////////////////////////////////////////////////////////
    // models
    ////////////////////////////////////////////////////////

    /**
     * Undocumented
     * @return Undocumented
     */
    public Set<ModelInfo> get_models(){
        Set<ModelInfo> x = new HashSet<>();
//        for(AbstractModel model : scenario.network.models.values()) {
//            switch(model.model_type){
//                case discrete_event:
//                    x.add(new ModelDiscreteEventInfo((AbstractDiscreteEventModel)model));
//                    break;
//                case discrete_time:
//                    x.add(new ModelDiscreteTimeInfo((AbstractDiscreteTimeModel) model));
//                    break;
//            }
//        }
        return x;
    }

    ////////////////////////////////////////////////////////
    // commodities
    ////////////////////////////////////////////////////////

    /**
     * Get the total number of commodities in the scenario.
     *
     * @return Undocumented
     */
    public int get_num_commodities(){
        return scenario.commodities.size();
    }

    /**
     * Get information for all commodities in the scenario.
     *
     * @return Undocumented
     * @see CommodityInfo
     */
    public Set<CommodityInfo> get_commodities(){
        Set<CommodityInfo> commInfo = new HashSet<>();
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

    /**
     * Undocumented
     * @return Undocumented
     */
    public Set<Long> get_commodity_ids(){
        return new HashSet(scenario.commodities.keySet());
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
     * @return Undocumented
     */
    public List<Long> get_subnetwork_ids(){
        return new ArrayList(scenario.subnetworks.keySet());
    }

    /**
     * Get list of all path ids (ie linear subnetworks that begin at a source)
     * @return Undocumented
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
     * @return a set of SubnetworkInfo
     * @see SubnetworkInfo
     */
    public Set<SubnetworkInfo> get_subnetworks(){
        Set<SubnetworkInfo> subnetInfo = new HashSet<>();
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
     * Undocumented
     * @return Undocumented
     */
    public List<Long> get_node_ids(){
        return new ArrayList(scenario.network.nodes.keySet());
    }

    /**
     * Returns a list where every entry is a list with entries [link_id,start_node,end_node]
     * @return Undocumented
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
     * @return Undocumented
     */
    public Map<Long, LinkInfo> get_links(){
        Map<Long,LinkInfo> linkInfo = new HashMap<>();
        for(Link link : scenario.network.links.values())
            linkInfo.put(link.getId(),new LinkInfo(link));
        return linkInfo;
    }

    /**
     * Get information for a specific link.
     * @param id Undocumented
     * @return Undocumented
     */
    public LinkInfo get_link_with_id(long id){
        Link link = scenario.network.links.get(id);
        return link==null ? null : new LinkInfo(link);
    }

    /**
     * Get the list of ids of all links in the network.
     * @return Undocumented
     */
    public List<Long> get_link_ids(){
        return new ArrayList(scenario.network.links.keySet());
    }

    /**
     * Get ids for all source links.
     * @return Undocumented
     */
    public List<Long> get_source_link_ids(){
        return scenario.network.links.values().stream()
                .filter(x->x.is_source)
                .map(x->x.getId())
                .collect(toList());
    }

    /**
     * Undocumented
     * @param rcid Undocumented
     * @return Undocumented
     */
    public List<Long> get_in_lanegroups_for_road_connection(long rcid){
        RoadConnection rc = scenario.network.get_road_connection(rcid);
        List<Long> lgids = new ArrayList<>();
        for(AbstractLaneGroup lg : rc.in_lanegroups)
            lgids.add(lg.id);
        return lgids;
    }

    /**
     * Undocumented
     * @param rcid Undocumented
     * @return Undocumented
     */
    public List<Long> get_out_lanegroups_for_road_connection(long rcid){
        RoadConnection rc = scenario.network.get_road_connection(rcid);
        List<Long> lgids = new ArrayList<>();
        for(AbstractLaneGroup lg : rc.out_lanegroups)
            lgids.add(lg.id);
        return lgids;
    }

    /**
     * Undocumented
     * @return Undocumented
     */
    public Map<Long,Set<Long>> get_link2lgs(){
        Map<Long,Set<Long>> lk2lgs = new HashMap<>();
        for(Link link : scenario.network.links.values())
            lk2lgs.put(link.getId(),link.lanegroups_flwdn.values().stream()
                    .map(x->x.id).collect(Collectors.toSet()));
        return lk2lgs;
    }

    ////////////////////////////////////////////////////////
    // demands / splits
    ////////////////////////////////////////////////////////

    /**
     * Get information for all demands in the scenario.
     * @return Undocumented
     */
    public List<DemandInfo> get_demands(){
        List<DemandInfo> x = new ArrayList<>();
        for(AbstractDemandProfile y : scenario.data_demands.values())
            x.add(new DemandInfo(y));
        return x;
    }

    /**
     * Get information for a specific demand.
     * @param typestr Undocumented
     * @param link_or_path_id Undocumented
     * @param commodity_id Undocumented
     * @return Undocumented
     */
    public DemandInfo get_demand_with_ids(String typestr,long link_or_path_id,long commodity_id){
        DemandType type = DemandType.valueOf(typestr);
        if(type==null)
            return null;
        AbstractDemandProfile dp = scenario.data_demands.get(new KeyCommodityDemandTypeId(commodity_id,link_or_path_id,type));
        return dp==null ? null : new DemandInfo(dp);
    }

    /**
     *  Undocumented
     */
    public void clear_all_demands(){

        if(scenario ==null)
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
     * @throws OTMException Undocumented
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
            dp.register_with_dispatcher(scenario.dispatcher);
        }

    }

    /**
     * Undocumented
     * @return Undocumented
     * @throws OTMException Undocumented
     */
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
                odinfo = new ODInfo(odpair, scenario);
                odmap.put(odpair,odinfo);
            }
            odinfo.add_demand_profile(demand_profile);
        }

        return new ArrayList(odmap.values());
    }

    /**
     * Undocumented
     * @return Undocumented
     */
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
     * @return Undocumented
     */
    public int get_num_sensors(){
        return scenario.sensors.size();
    }

    /**
     * Get information for all sensors in the scenario.
     * @return Undocumented
     */
    public List<SensorInfo> get_sensors(){
        List<SensorInfo> x = new ArrayList<>();
        for(AbstractSensor y : scenario.sensors.values())
            x.add(new SensorInfo(y));
        return x;
    }

    /**
     * Get information for a specific sensor.
     * @param id Undocumented
     * @return Undocumented
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

    /**
     *
     * @param id Undocumented
     * @return Undocumented
     */
    public AbstractController get_actual_controller_with_id(long id){
        return scenario.controllers.get(id);
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
