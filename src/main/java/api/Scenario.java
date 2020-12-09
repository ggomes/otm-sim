package api;

import commodity.Subnetwork;
import common.*;
import control.AbstractController;
import dispatch.Dispatcher;
import dispatch.EventCreateVehicle;
import dispatch.EventDemandChange;
import models.vehicle.spatialq.EventTransitToWaiting;
import error.OTMException;
import models.vehicle.spatialq.MesoLaneGroup;
import models.vehicle.spatialq.MesoVehicle;
import output.animation.AnimationInfo;
import profiles.SplitMatrixProfile;
import utils.OTMUtils;

import java.util.*;

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
            lk2lgs.put(link.getId(),link.lanegroups_flwdn.stream()
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

        if(myapi.scn ==null)
            return;

        // delete sources from links
        for(Link link : myapi.scn.network.links.values()) {
            if (link.demandGenerators == null || link.demandGenerators.isEmpty())
                continue;
            link.demandGenerators.clear();
        }

        // delete all EventCreateVehicle and EventDemandChange from dispatcher
        if(myapi.scn.dispatcher!=null) {
            myapi.scn.dispatcher.remove_events_of_type(EventCreateVehicle.class);
            myapi.scn.dispatcher.remove_events_of_type(EventDemandChange.class);
        }

    }

    /**
     * Integrate the demands to obtain the total number of trips that will take place.
     * @return The number of trips.
     */
    public double get_total_trips() {
        return myapi.scn.network.links.values().stream()
                .filter(link->link.demandGenerators !=null && !link.demandGenerators.isEmpty())
                .flatMap(link->link.demandGenerators.stream())
                .map(gen->gen.get_total_trips())
                .reduce(0.0,Double::sum);
    }

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

}
