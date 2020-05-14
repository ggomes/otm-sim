package api;

import actuator.AbstractActuator;
import actuator.ActuatorSignal;
import api.info.*;
import commodity.Commodity;
import commodity.Subnetwork;
import common.Link;
import common.Node;
import common.RoadConnection;
import control.AbstractController;
import control.sigint.ControllerSignalPretimed;
import dispatch.Dispatcher;
import dispatch.EventCreateVehicle;
import dispatch.EventDemandChange;
import models.vehicle.spatialq.EventTransitToWaiting;
import error.OTMErrorLog;
import error.OTMException;
import keys.DemandType;
import keys.KeyCommodityDemandTypeId;
import common.AbstractLaneGroup;
import models.vehicle.spatialq.MesoLaneGroup;
import models.vehicle.spatialq.MesoVehicle;
import output.animation.AnimationInfo;
import profiles.AbstractDemandProfile;
import profiles.DemandProfile;
import profiles.SplitMatrixProfile;
import sensor.AbstractSensor;
import utils.OTMUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

/**
 * Methods for querying and modifying a scenario.
 */
public class Scenario {

    private api.OTM myapi;

    protected Scenario(api.OTM myapi){
        this.myapi = myapi;
    }

    ////////////////////////////////////////////////////////
    // models
    ////////////////////////////////////////////////////////

    /**
     * Get model coverage.
     * @return a set of model info objects
     * @see ModelInfo
     */
    public Set<ModelInfo> get_models(){
        return myapi.scn.network.models.values().stream()
                .map(model->new ModelInfo(model))
                .collect(toSet());
    }

    ////////////////////////////////////////////////////////
    // commodities
    ////////////////////////////////////////////////////////

    /**
     * Get the total number of commodities in the scenario.
     * @return integer number of commodities.
     */
    public int get_num_commodities(){
        return myapi.scn.commodities.size();
    }

    /**
     * Get information for all commodities in the scenario.
     * @return Set of commodity info
     * @see CommodityInfo
     */
    public Set<CommodityInfo> get_commodities(){
        Set<CommodityInfo> commInfo = new HashSet<>();
        for(Commodity comm : myapi.scn.commodities.values())
            commInfo.add(new CommodityInfo(comm));
        return commInfo;
    }

    /**
     * Get information for a specific commodity.
     * @param  id Integer commodity id
     * @return A CommodityInfo object
     * @see CommodityInfo
     */
    public CommodityInfo get_commodity_with_id(long id){
        Commodity comm = myapi.scn.commodities.get(id);
        return comm==null? null : new CommodityInfo(comm);
    }

    /**
     * Get commodity ids.
     * @return set of commodity ids
     */
    public Set<Long> get_commodity_ids(){
        return new HashSet(myapi.scn.commodities.keySet());
    }

    ////////////////////////////////////////////////////////
    // subnetworks and paths
    ////////////////////////////////////////////////////////

    /**
     * Get the total number of subnetworks in the scenario.
     * @return integer number of subnetworks
     */
    public int get_num_subnetworks(){
        return myapi.scn.subnetworks.size();
    }

    /**
     * Get subnetwork ids
     * @return Set of subnetwork ids
     */
    public Set<Long> get_subnetwork_ids(){
        return new HashSet(myapi.scn.subnetworks.keySet());
    }

    /**
     * Get set of all path ids (ie linear subnetworks that begin at a source)
     * @return Set of path ids
     */
    public Set<Long> get_path_ids(){
        return myapi.scn.subnetworks.values().stream()
                .filter(x->x.isPath())
                .map(x->x.getId())
                .collect(toSet());
    }

    /**
     * Get information for all subnetworks in the scenario.
     * @return a set of SubnetworkInfo
     * @see SubnetworkInfo
     */
    public Set<SubnetworkInfo> get_subnetworks(){
        Set<SubnetworkInfo> subnetInfo = new HashSet<>();
        for(Subnetwork subnet : myapi.scn.subnetworks.values())
            subnetInfo.add(new SubnetworkInfo(subnet));
        return subnetInfo;
    }

    /**
     * Get information for a specific subnetwork.
     * @param  id Integer subnetwork id
     * @return A SubnetworkInfo object
     * @see SubnetworkInfo
     */
    public SubnetworkInfo get_subnetwork_with_id(long id){
        Subnetwork subnet = myapi.scn.subnetworks.get(id);
        return subnet==null? null : new SubnetworkInfo(subnet);
    }

    public long add_subnetwork(String name, Set<Long> linkids,Set<Long> comm_ids) throws OTMException {
        Long subnetid = myapi.scn.subnetworks.keySet().stream().max(Long::compare).get() + 1;
        Subnetwork newsubnet = new Subnetwork(subnetid,name,linkids,comm_ids,myapi.scn);
        myapi.scn.subnetworks.put(subnetid,newsubnet);
        return subnetid;
    }

    public boolean remove_subnetwork(long subnetid) {
        if(!myapi.scn.subnetworks.containsKey(subnetid))
            return false;
        myapi.scn.subnetworks.remove(subnetid);
        return true;
    }

    public void subnetwork_remove_links(long subnetid,Set<Long> linkids) throws OTMException {
        if(!myapi.scn.subnetworks.containsKey(subnetid))
            throw new OTMException("Bad subnetwork id in subnetwork_delete_link");

        if(!myapi.scn.network.links.keySet().containsAll(linkids))
            throw new OTMException("Bad link id in subnetwork_delete_link");

        Set<Link> links = linkids.stream().map(i->myapi.scn.network.links.get(i)).collect(toSet());
        myapi.scn.subnetworks.get(subnetid).remove_links(links);
    }

    public void subnetwork_add_links(long subnetid,Set<Long> linkids) throws OTMException {
        if(!myapi.scn.subnetworks.containsKey(subnetid))
            throw new OTMException("Bad subnetwork id in subnetwork_add_link");

        if(!myapi.scn.network.links.keySet().containsAll(linkids))
            throw new OTMException("Bad link id in subnetwork_add_link");

        Set<Link> links = linkids.stream().map(i->myapi.scn.network.links.get(i)).collect(toSet());
        myapi.scn.subnetworks.get(subnetid).add_links(links);
    }

    ////////////////////////////////////////////////////////
    // network
    ////////////////////////////////////////////////////////

    /**
     * Get the total number of links in the scenario.
     * @return an integer.
     */
    public int get_num_links(){
        return myapi.scn.network.links.size();
    }

    /**
     * Get the total number of nodes in the scenario.
     * @return an integer.
     */
    public int get_num_nodes(){
        return myapi.scn.network.nodes.size();
    }

    /**
     * Get node ids
     * @return Set of node ids.
     */
    public Set<Long> get_node_ids(){
        return new HashSet(myapi.scn.network.nodes.keySet());
    }

    /**
     * Get information for all nodes in the scenario.
     * @return Map from node id to NodeInfo
     * @see NodeInfo
     */
    public Map<Long, NodeInfo> get_nodes(){
        Map<Long,NodeInfo> nodeInfo = new HashMap<>();
        for(Node node : myapi.scn.network.nodes.values())
            nodeInfo.put(node.getId(),new NodeInfo(node));
        return nodeInfo;
    }

    /**
     * Get information for a specific node.
     * @param node_id Id of the requested node.
     * @return a NodeInfo object
     * @see NodeInfo
     */
    public NodeInfo get_node_with_id(long node_id){
        Node node = myapi.scn.network.nodes.get(node_id);
        return node==null ? null : new NodeInfo(node);
    }

    /**
     * Returns a set where every entry is a list with entries [link_id,start_node,end_node]
     * @return A set of lists
     */
    public Set<List<Long>> get_link_connectivity(){
        Set<List<Long>> X = new HashSet<>();
        for(Link link : myapi.scn.network.links.values()){
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
     * @return Map from link id to LinkInfo
     * @see LinkInfo
     */
    public Map<Long,LinkInfo> get_links(){
        return myapi.scn.network.links.values().stream().collect(Collectors.toMap(x->x.getId(),x->new LinkInfo(x)));
    }

    /**
     * Get information for a specific link.
     * @param link_id Id of the requested link.
     * @return a LinkInfo object
     * @see LinkInfo
     */
    public LinkInfo get_link_with_id(long link_id){
        Link link = myapi.scn.network.links.get(link_id);
        return link==null ? null : new LinkInfo(link);
    }

    /**
     * Get the set of ids of all links in the network.
     * @return A set of ids
     */
    public Set<Long> get_link_ids(){
        return new HashSet(myapi.scn.network.links.keySet());
    }

    /**
     * Get ids for all source links.
     * @return A set of ids.
     */
    public Set<Long> get_source_link_ids(){
        return myapi.scn.network.links.values().stream()
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
        RoadConnection rc = myapi.scn.network.get_road_connection(rcid);
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
        RoadConnection rc = myapi.scn.network.get_road_connection(rcid);
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
        for(Link link : myapi.scn.network.links.values())
            lk2lgs.put(link.getId(),link.lanegroups_flwdn.values().stream()
                    .map(x->x.id).collect(toSet()));
        return lk2lgs;
    }

    public void set_road_type(long link_id,String str){
        if(!myapi.scn.network.links.containsKey(link_id))
            return;
        Link link = myapi.scn.network.links.get(link_id);
        Link.RoadType new_road_type = Link.RoadType.valueOf(str);
        if(new_road_type!=null)
            link.road_type = new_road_type;
    }

    public Node create_node(float xcoord,float ycoord){
        long id = myapi.scn.network.nodes.keySet().stream().max(Comparator.naturalOrder()).get();
        return new common.Node(myapi.scn.network,id,xcoord,ycoord,false);
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
        Link link = myapi.scn.network.links.get(link_id);
        MesoLaneGroup lg = (MesoLaneGroup) link.lanegroups_flwdn.values().iterator().next();
        return new Queues(lg.waiting_queue.num_vehicles(),lg.transit_queue.num_vehicles());
    }

    /** Set the number of vehicles in a link
     * This only works for a single commodity scenarios, and single lane group links.
     * @param link_id
     * @param numvehs_waiting
     * @param numvehs_transit
     */
    public void set_link_vehicles(long link_id, int numvehs_waiting,int numvehs_transit) throws Exception {

        if(myapi.scn.commodities.size()>1)
            throw new Exception("Cannot call set_link_vehicles on multi-commodity networks");

        if(!myapi.scn.network.links.containsKey(link_id))
            throw new Exception("Bad link id");

        Link link = myapi.scn.network.links.get(link_id);

        if(link.lanegroups_flwdn.size()>1)
            throw new Exception("Cannot call set_link_vehicles on multi-lane group links");

//        if(link.model.type!= ModelType.VehicleMeso)
//            throw new Exception("Cannot call set_link_vehicles on non-meso models");

        long comm_id = myapi.scn.commodities.keySet().iterator().next();
        MesoLaneGroup lg = (MesoLaneGroup) link.lanegroups_flwdn.values().iterator().next();
        common.SplitInfo splitinfo = lg.link.commodity2split.get(comm_id);

        // transit queue ................
        models.vehicle.spatialq.Queue tq = lg.transit_queue;
        tq.clear();
        for(int i=0;i<numvehs_transit;i++) {
            MesoVehicle vehicle = new MesoVehicle(comm_id, null);

            // sample the split ratio to decide where the vehicle will go
            Long next_link_id = splitinfo.sample_output_link();
            vehicle.set_next_link_id(next_link_id);

            // set the vehicle's lane group and key
            vehicle.lg = lg;
            vehicle.my_queue = tq;

            // add to lane group (as in lg.add_vehicle_packet)
            tq.add_vehicle(vehicle);

            // register_with_dispatcher dispatch to go to waiting queue
            Dispatcher dispatcher = myapi.scn.dispatcher;
            float timestamp = myapi.scn.get_current_time();
            float transit_time_sec = (float) OTMUtils.random_zero_to_one()*lg.transit_time_sec;
            dispatcher.register_event(new EventTransitToWaiting(dispatcher,timestamp + transit_time_sec,vehicle));
        }

        // waiting queue .................
        models.vehicle.spatialq.Queue wq = lg.waiting_queue;
        wq.clear();
        for(int i=0;i<numvehs_waiting;i++) {
            MesoVehicle vehicle = new MesoVehicle(comm_id, null);

            // sample the split ratio to decide where the vehicle will go
            Long next_link_id = splitinfo.sample_output_link();
            vehicle.set_next_link_id(next_link_id);

            // set the vehicle's lane group and key
            vehicle.lg = lg;
            vehicle.my_queue = wq;

            // add to lane group (as in lg.add_vehicle_packet)
            wq.add_vehicle(vehicle);
        }

    }

    ////////////////////////////////////////////////////////
    // demands / splits
    ////////////////////////////////////////////////////////

    /**
     * Get information for all demands in the scenario.
     * @return A set of DemandInfo
     * @see DemandInfo
     */
    public Map<Long,Set<DemandInfo>> get_demands(){
        Map<Long,Set<DemandInfo>> x = new HashMap<>();
        for(AbstractDemandProfile p : myapi.scn.data_demands.values()) {
            long link_id = p.source.link.getId();
            Set<DemandInfo> y;
            if(x.containsKey(link_id))
                y = x.get(link_id);
            else {
                y = new HashSet<>();
                x.put(link_id,y);
            }
            y.add(new DemandInfo(p));
        }
        return x;
    }

    /**
     * Get information for a specific demand.
     * @param typestr 'pathfull' or 'pathless' (NOTE: Why is this an input???)
     * @param link_or_path_id Id of the source link or path
     * @param commodity_id Id of the commodity
     * @return A DemandInfo object
     * @see DemandInfo
     */
    public DemandInfo get_demand_with_ids(String typestr,long link_or_path_id,long commodity_id){
        DemandType type = DemandType.valueOf(typestr);
        if(type==null)
            return null;
        AbstractDemandProfile dp = myapi.scn.data_demands.get(new KeyCommodityDemandTypeId(commodity_id,link_or_path_id,type));
        return dp==null ? null : new DemandInfo(dp);
    }

    /**
     *  Clear all demands in the scenario.
     */
    public void clear_all_demands(){

        if(myapi.scn ==null)
            return;

        // delete sources from links
        for(Link link : myapi.scn.network.links.values())
            link.sources = new HashSet<>();

        // delete all EventCreateVehicle and EventDemandChange from dispatcher
        if(myapi.scn.dispatcher!=null) {
            myapi.scn.dispatcher.remove_events_for_recipient(EventCreateVehicle.class);
            myapi.scn.dispatcher.remove_events_for_recipient(EventDemandChange.class);
        }

        // delete all demand profiles
        if(myapi.scn.data_demands!=null)
            myapi.scn.data_demands.clear();
    }

    /**
     * Set or override a demand value for a path.
     * Use this method to set a demand profile of a given commodity on a given path.
     * The profile is a piecewise constant function starting a time "start_time" and with
     * sample time "dt". The values are given by the "values" array. The value before
     * before "start_time" is zero, and the last value in the array is held into positive
     * infinity time.
     * This method will override any existing demands for that commodity and path.
     *
     * @param path_id : [long] integer id of the subnetwork
     * @param commodity_id : [long] integer id of the commodity
     * @param start_time : [float] start time for the demand profile in seconds after midnight.
     * @param dt : [float] step time for the profile in seconds.
     * @param values : [array of doubles] list of values for the piecewise continuous profile.
     * @throws OTMException Undocumented
     */
    public void add_pathfull_demand(long path_id, long commodity_id, float start_time, float dt, List<Double> values) throws OTMException {

        // create a demand profile
        Subnetwork path = myapi.scn.subnetworks.get(path_id);
        if(path==null)
            throw new OTMException("Bad path id");

        Commodity commodity = myapi.scn.commodities.get(commodity_id);
        if(commodity==null)
            throw new OTMException("Bad commodity id");

        add_demand( new DemandProfile(path,commodity,start_time, dt, values) );

    }

    /**
     * Set or override a demand on a source.
     * Use this method to set a demand profile of a given commodity on a given source link..
     * The profile is a piecewise constant function starting a time "start_time" and with
     * sample time "dt". The values are given by the "values" array. The value before
     * before "start_time" is zero, and the last value in the array is held into positive
     * infinity time.
     * This method will override any existing demands for that commodity and source.
     *
     * @param link_id : [long] integer id of the source link
     * @param commodity_id : [long] integer id of the commodity
     * @param start_time : [float] start time for the demand profile in seconds after midnight.
     * @param dt : [float] step time for the profile in seconds.
     * @param values : [array of doubles] list of values for the piecewise continuous profile.
     * @throws OTMException Undocumented
     */
    public void add_pathless_demand(long link_id, long commodity_id, float start_time, float dt, List<Double> values) throws OTMException {

        // create a demand profile
        Link link = myapi.scn.network.links.get(link_id);
        if(link==null)
            throw new OTMException("Bad link id");

        Commodity commodity = myapi.scn.commodities.get(commodity_id);
        if(commodity==null)
            throw new OTMException("Bad commodity id");

        add_demand(new DemandProfile(link,commodity,start_time, dt, values));
    }

    /**
     * Get OD matrix information for this scenario
     * @return List of ODInfo objects
     * @throws OTMException Undocumented
     */
    public List<ODInfo> get_od_info() throws OTMException {

        Map<ODPair,ODInfo> odmap = new HashMap<>();

        for(AbstractDemandProfile demand_profile : myapi.scn.data_demands.values()){

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
                odinfo = new ODInfo(odpair, myapi.scn);
                odmap.put(odpair,odinfo);
            }
            odinfo.add_demand_profile(demand_profile);
        }

        return new ArrayList(odmap.values());
    }

    /**
     * Integrate the demands to obtain the total number of trips that will take place.
     * @return The number of trips.
     */
    public double get_total_trips() {
        return myapi.scn.data_demands.values().stream()
                .map(x->x.get_total_trips())
                .reduce(0.0,Double::sum);
    }

    public Map<Long,Set<api.info.SplitInfo>> get_splits(){
        Map<Long,Set<api.info.SplitInfo>> x = new HashMap<>();
        for(Node node : myapi.scn.network.nodes.values().stream().filter(n->n.splits!=null).collect(Collectors.toSet())) {
            Set<api.info.SplitInfo> y = new HashSet<>();
            x.put(node.getId(),y);
            for (SplitMatrixProfile p : node.splits.values())
                y.add(new api.info.SplitInfo(p));
        }
        return x;
    }

//    public void add_splits(long in_link_id,long commodity_id,float start_time,float dt,Map<Long,List<Double>> outlink2splits) throws OTMException {
//
//        Link inlink = myapi.scn.network.links.get(in_link_id);
//        if(inlink==null)
//            throw new OTMException("Bad link id");
//
//        Commodity commodity = myapi.scn.commodities.get(commodity_id);
//        if(commodity==null)
//            throw new OTMException("Bad commodity id");
//
//        Node node = inlink.end_node;
//
//        SplitMatrixProfile smp = new SplitMatrixProfile(commodity_id,node,in_link_id,start_time,dt);
//        for(Map.Entry<Long,List<Double>> e : outlink2splits.entrySet())
//            smp.add_splits(e.getKey(),e.getValue());
//
//        node.set_node_split();
//    }

    ////////////////////////////////////////////////////////
    // sensors
    ////////////////////////////////////////////////////////

    /**
     * Get the total number of sensors in the scenario.
     * @return Number of sensors
     */
    public int get_num_sensors(){
        return myapi.scn.sensors.size();
    }

    /**
     * Get information for all sensors in the scenario.
     * @return A set of SensorInfo
     * @see SensorInfo
     */
    public Set<SensorInfo> get_sensors(){
        Set<SensorInfo> x = new HashSet<>();
        for(AbstractSensor y : myapi.scn.sensors.values())
            x.add(new SensorInfo(y));
        return x;
    }

    /**
     * Get information for a specific sensor.
     * @param id Id of the requested sensor
     * @return A SensorInfo object
     * @see SensorInfo
     */
    public SensorInfo get_sensor_with_id(long id){
        AbstractSensor sensor = myapi.scn.sensors.get(id);
        return sensor==null ? null : new SensorInfo(sensor);
    }

    ////////////////////////////////////////////////////////
    // controllers
    ////////////////////////////////////////////////////////

    /**
     * Get the total number of controllers in the scenario.
     * @return an integer.
     */
    public int get_num_controllers(){
        return myapi.scn.controllers.size();
    }

    /**
     * Get information for all controllers in the scenario.
     * @return a set of ControllerInfo
     * @see ControllerInfo
     */
    public Set<ControllerInfo> get_controller_infos(){
        Set<ControllerInfo> X = new HashSet<>();
        for(AbstractController cntrl : myapi.scn.controllers.values()) {
            if( cntrl instanceof ControllerSignalPretimed) {
                X.add(new ControllerSignalPretimedInfo((ControllerSignalPretimed)cntrl));
            }
            else {
                X.add(new ControllerInfo(cntrl));
            }
        }
        return X;
    }

    /**
     * Get information for a specific controller.
     * @param id : unique id of the controller.
     * @return A ControllerInfo object
     * @see ControllerInfo
     */
    public ControllerInfo get_controller_info(long id){
        AbstractController controller = myapi.scn.controllers.get(id);
        return controller==null ? null : new ControllerInfo(controller);
    }

    /**
     * Get a controller object. This object can then be used to modify the control algorithm.
     * @param id : unique id of the controller.
     * @return AbstractController
     */
    public AbstractController get_actual_controller_with_id(long id){
        return myapi.scn.controllers.containsKey(id) ? myapi.scn.controllers.get(id) : null;
    }

    public Set<Long> get_controller_ids(){
        return myapi.scn.controllers.keySet();
    }

    ////////////////////////////////////////////////////////
    // actuators
    ////////////////////////////////////////////////////////

    /**
     * Get the total number of actuators in the scenario
     * @return an integer.
     */
    public int get_num_actuators(){
        return myapi.scn.actuators.size();
    }

    /**
     * Get information for all actuators in the scenario.
     * @return a set of ActuatorInfo
     * @see ActuatorInfo
     */
    public Set<ActuatorInfo> get_actuators(){
        Set<ActuatorInfo> x = new HashSet<>();
        for(AbstractActuator y : myapi.scn.actuators.values())
            x.add(create_actuator_info(y));
        return x;
    }

    /**
     * Get information for a specific actuator.
     * @param id : [long] integer id of the actuator.
     * @return A ActuatorInfo object
     * @see ActuatorInfo
     */
    public ActuatorInfo get_actuator_with_id(long id){
        return create_actuator_info( myapi.scn.actuators.get(id) );
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
        return new AnimationInfo(myapi.scn,link_ids);
    }

    /**
     * Undocumented
     * @return Undocumented
     * @throws OTMException Undocumented
     */
    public AnimationInfo get_animation_info() throws OTMException {
        return new AnimationInfo(myapi.scn);
    }

    ////////////////////////////////////////////////////////
    // private
    ////////////////////////////////////////////////////////

    private void add_demand(DemandProfile dp) throws OTMException {

        // validate
        OTMErrorLog errorLog = new OTMErrorLog();
        dp.validate(errorLog);
        if (errorLog.haserror())
            throw new OTMException(errorLog.format_errors());

        // if a similar demand already exists, then delete it
        if(myapi.scn.data_demands.containsKey(dp.get_key())){
            AbstractDemandProfile old_dp = myapi.scn.data_demands.get(dp.get_key());
            if(myapi.dispatcher!=null)
                myapi.dispatcher.remove_events_for_recipient(EventDemandChange.class,old_dp);
            myapi.scn.data_demands.remove(dp.get_key());

            // remove it and its source from the link
            old_dp.source.link.sources.remove(old_dp.source);
        }

        // add to scenario
        myapi.scn.data_demands.put(dp.get_key(),dp);

        if(myapi.scn.is_initialized) {
            // initialize
            dp.initialize(myapi.scn);

            // send to dispatcher
            dp.register_with_dispatcher(myapi.scn.dispatcher);
        }

    }

    private ActuatorInfo create_actuator_info(AbstractActuator actuator){

        if(actuator==null)
            return null;

        if(actuator instanceof ActuatorSignal)
            return new SignalInfo((ActuatorSignal) actuator);

        return new ActuatorInfo(actuator);
    }
}
