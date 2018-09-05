package common;

import actuator.AbstractActuator;
import commodity.Commodity;
import error.OTMErrorLog;
import error.OTMException;
import packet.AbstractPacketLaneGroup;
import runner.RunParameters;
import runner.Scenario;
import utils.OTMUtils;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractLaneGroup implements Comparable<AbstractLaneGroup> {

    public long id;
    public Link link;

    // map from outlink to road-connection. For one-to-one links with no road connection defined,
    // this returns a null.
    protected Map<Long,RoadConnection> outlink2roadconnection;

    public float length;
    public Set<Integer> lanes;

    // parameters
    public float max_vehicles;      // largest number of vehicles that fit in this lane group

    public AbstractActuator actuator; // NOTE: WHY IS THERE ONLY ONE?

    // flow accumulators
    public FlowAccumulator global_flow_accumulator;
    public Map<Long,FlowAccumulator> commodity_flow_accumulators;     // commodity_id -> FlowAccumulator

    ///////////////////////////////////////////////////
    // abstract methods
    ///////////////////////////////////////////////////


    abstract public void add_commodity(Commodity commodity);

    abstract public void add_native_vehicle_packet(float timestamp, AbstractPacketLaneGroup vp) throws OTMException;

    abstract public void exiting_roadconnection_capacity_has_been_modified();

    /**
     * An event signals an opportunity to release a vehicle packet. The lanegroup must,
     * 1. construct packets to be released to each of the lanegroups reached by each of it's
     *    road connections.
     * 2. check what portion of each of these packets will be accepted. Reduce the packets
     *    if necessary.
     * 3. call next_link.add_native_vehicle_packet for each reduces packet.
     * 4. remove the vehicle packets from this lanegroup.
     */
    abstract public void release_vehicle_packets(float timestamp) throws OTMException;

    abstract public double get_supply();

    /** Return the total number of vehicles in this lane group with the
     * given commodity id. commodity_id==null means return total over all
     * commodities.
     */
    abstract public float vehicles_for_commodity(Long commodity_id);

    abstract public float get_current_travel_time();

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public AbstractLaneGroup(Link link,Set<Integer> lanes,Set<RoadConnection> out_rcs){
        this.link = link;
        this.id = OTMUtils.get_lanegroup_id();
        this.lanes = lanes;
        this.outlink2roadconnection = new HashMap<>();

        for(RoadConnection rc : out_rcs)
            outlink2roadconnection.put(rc.end_link.id,rc);
    }

    public void delete(){
        link = null;
        outlink2roadconnection = null;
        lanes = null;
        actuator = null;
        global_flow_accumulator = null;
        commodity_flow_accumulators = null;
    }

    public void validate(OTMErrorLog errorLog) {

        if(lanes.isEmpty())
            errorLog.addError("lanes.isEmpty()");
        else{
            // lanes are on the link
            if(Collections.max(lanes)>link.total_lanes)
                errorLog.addError("Collections.max(lanes)>link.get_num_lanes()");
            if(Collections.min(lanes)<1)
                errorLog.addError("Collections.min(lanes)<1");
        }

        // out_road_connections all lead to links that are immediately downstream
        Collection dwn_links = link.end_node.out_links.values().stream().map(x->x.id).collect(Collectors.toSet());
        if(!dwn_links.containsAll(outlink2roadconnection.keySet()))
            errorLog.addError("some outlinks are not immediately downstream");

        // check that all lanes have the same length
//        boolean isfirst = true;
//        float L=Float.NaN;
//        for(int lane : lanes ) {
//            if(isfirst){
//                L=link.get_length_for_lane(lane);
//                isfirst=false;
//            }
////            else
////                if(!OTMUtils.approximately_equals(L,link.get_length_for_lane(lane)))
////                    scenario.error_log.addError("lanegroup has lanes of unequal length: lanegroup:" + id + " link:" + link.getId());
//        }
    }

    public void initialize(Scenario scenario, RunParameters runParams) throws OTMException {
        if(global_flow_accumulator!=null)
            global_flow_accumulator.reset();
        if(commodity_flow_accumulators!=null)
            commodity_flow_accumulators.values().forEach(x->x.reset());
    }

    public void set_road_params(jaxb.Roadparam r){
        // all lanes in the lanegroup are expected to have the same length
        length = link.get_length_for_lane(Collections.min(lanes));
        max_vehicles = r.getJamDensity()*length*lanes.size()/1000;
    }

    public FlowAccumulator request_flow_accumulator(Long commodity_id){
        if(commodity_id==null){
            if(global_flow_accumulator==null)
                global_flow_accumulator = new FlowAccumulator(this);
            return global_flow_accumulator;
        }
        if(commodity_flow_accumulators==null)
            commodity_flow_accumulators = new HashMap<>();
        if(commodity_flow_accumulators.containsKey(commodity_id))
            return commodity_flow_accumulators.get(commodity_id);
        FlowAccumulator r = new FlowAccumulator(this,commodity_id);
        commodity_flow_accumulators.put(commodity_id,r);
        return r;
    }

    ///////////////////////////////////////////////////
    // get topological
    ///////////////////////////////////////////////////

    public int num_lanes(){
        return lanes.size();
    }

    // return length in meters
    public float length() {
        /** NOTE THIS SHOULD VARY ACCORDING THE LINK TYPE **/
        return link.get_length_for_lane(1);
    }

    public Set<Long> get_dwn_links(){
        return outlink2roadconnection.keySet();
    }

    public boolean is_link_reachable(Long link_id){
        return outlink2roadconnection.containsKey(link_id);
    }

    // returns null if either the outlink is unknown or the lanegroup is one-to-one
    public RoadConnection get_roadconnection_for_outlink(Long link_id){
        return link_id==null? null : outlink2roadconnection.get(link_id);
    }

    public Set<AbstractLaneGroup> get_accessible_lgs_in_outlink(Link out_link){

        // if the end node is one to one, then all lanegroups in the next link are equally accessible
        if(link.end_node.is_many2one) {
            if (link.outlink2lanegroups.containsKey(out_link.getId()))
                return new HashSet<>(out_link.lanegroups.values());     // all downstream lanegroups are accessible
            else
                return null;
        }

        // otherwise, get the road connection connecting this lg to out_link
        RoadConnection rc = outlink2roadconnection.get(out_link.getId());

        // return lanegroups connected to by this road connection
        return out_link.get_lanegroups_for_lanes(rc.end_link_from_lane,rc.end_link_to_lane);

    }

    public Set<AbstractLaneGroup> get_my_neighbors(){
        if(link.lanegroups.size()<2)
            return null;

        int lane_to_left = Collections.min(lanes)-1;
        int lane_to_right = Collections.max(lanes)+1;

        return link.lanegroups.values().stream()
                .filter(lg->lg.lanes.contains(lane_to_left) || lg.lanes.contains(lane_to_right))
                .collect(Collectors.toSet());
    }

    ///////////////////////////////////////////////////
    // get state
    ///////////////////////////////////////////////////

    public final float get_total_vehicles() {
        return vehicles_for_commodity(null);
    }

    public final double get_space_per_lane() {
        return get_space()/num_lanes();
    }

    public final float get_space() {
        return max_vehicles-vehicles_for_commodity(null);
    }

    ///////////////////////////////////////////////////
    // set
    ///////////////////////////////////////////////////

    @Override
    public String toString() {
        String str = "";
        str += "id " + id + "\n";
        str += "\tlink " + link.id + "\n";
        str += "\tlanes " + OTMUtils.comma_format(new ArrayList(lanes)) + "\n";
//        str += "\tout road connections\n";
//        for(RoadConnection rc : outlink2roadconnection.values())
//            str += "\t\tlink " + rc.end_link.id + ", lanes " + rc.end_link_from_lane + "-" + rc.end_link_to_lane + "\n";
        return str;
    }

    ///////////////////////////////////////////////////
    // update
    ///////////////////////////////////////////////////

    protected void update_flow_accummulators(Long commodity_id,double num_vehicles){
        // tell the flow accumulators
        if(commodity_flow_accumulators!=null && commodity_flow_accumulators.containsKey(commodity_id))
            commodity_flow_accumulators.get(commodity_id).increment(num_vehicles);
        if(global_flow_accumulator!=null)
            global_flow_accumulator.increment(num_vehicles);

    }

    ///////////////////////////////////////////////////////////////
    // static
    ///////////////////////////////////////////////////////////////


    public int distance_to_lanes( int min_lane,int max_lane){
        int lg_min_lane = lanes.stream().mapToInt(x->x).min().getAsInt();
        int lg_max_lane = lanes.stream().mapToInt(x->x).max().getAsInt();
        int distance = Math.max(lg_min_lane-max_lane,min_lane-lg_max_lane);
        return Math.max(distance,0);
    }

    @Override
    public int compareTo(AbstractLaneGroup that) {

        int this_start = this.lanes.stream().min(Integer::compareTo).get();
        int that_start = that.lanes.stream().min(Integer::compareTo).get();
        if(this_start < that_start)
            return -1;
        if(that_start < this_start)
            return 1;

        int this_end = this.lanes.stream().max(Integer::compareTo).get();
        int that_end = that.lanes.stream().max(Integer::compareTo).get();
        if(this_end < that_end)
            return -1;
        if(that_end < this_end)
            return 1;

        return 0;
    }

}
