/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models;

import actuator.AbstractActuator;
import commodity.Commodity;
import common.FlowAccumulatorState;
import common.Link;
import common.RoadConnection;
import error.OTMErrorLog;
import error.OTMException;
import geometry.FlowDirection;
import geometry.Side;
import keys.KeyCommPathOrLink;
import packet.AbstractPacketLaneGroup;
import runner.RunParameters;
import runner.Scenario;
import utils.OTMUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public abstract class AbstractLaneGroup implements Comparable<AbstractLaneGroup> {

    public final long id;
    public Link link;
    public final Side side;               // inner, stay, or outer
    public final FlowDirection flwdir;
    public int start_lane_up;       // counted with respect to upstream boundary
    public int start_lane_dn;       // counted with respect to downstream boundary
    public final int num_lanes;

    public AbstractLaneGroup neighbor_in;       // lanegroup down and in
    public AbstractLaneGroup neighbor_out;      // lanegroup down and out
    public AbstractLaneGroup neighbor_up_in;    // lanegroup up and in (stay lanes only)
    public AbstractLaneGroup neighbor_up_out;   // lanegroup up and out (stay lanes only)

    // set of keys for states in this lanegroup
    public Set<KeyCommPathOrLink> states;   // TODO MOVE THIS TO DISCRETE TIME ONLY?

    public float length;

    // parameters
    public float max_vehicles;      // largest number of vehicles that fit in this lane group

    public AbstractActuator actuator;

    // flow accumulator
    public FlowAccumulatorState flw_acc;

    // map from outlink to road-connection. For one-to-one links with no road connection defined,
    // this returns a null.
    public Map<Long, RoadConnection> outlink2roadconnection;

    // state to the road connection it must use (should be avoided in the one-to-one case)
    public Map<KeyCommPathOrLink,Long> state2roadconnection;

    // target lane group to direction
    public Map<KeyCommPathOrLink,Side> state2lanechangedirection = new HashMap<>();

    ///////////////////////////////////////////////////
    // abstract methods
    ///////////////////////////////////////////////////

    abstract public void allocate_state();
    abstract public double get_supply();
    abstract public void add_vehicle_packet(float timestamp, AbstractPacketLaneGroup vp, Long nextlink_id) throws OTMException;
    abstract public void exiting_roadconnection_capacity_has_been_modified(float timestamp);
    abstract public void set_max_speed_mps(Float max_speed_mps) throws OTMException;
    abstract public void set_max_flow_vpspl(Float max_flow_vpspl) throws OTMException;
    abstract public void set_max_density_vpkpl(Float max_density_vpkpl) throws OTMException;

    /** Return the total number of vehicles in this lane group with the
     * given commodity id. commodity_id==null means return total over all
     * commodities.
     */
    abstract public float vehs_dwn_for_comm(Long comm_id);
    abstract public float vehs_in_for_comm(Long comm_id);
    abstract public float vehs_out_for_comm(Long comm_id);
    abstract public float get_current_travel_time();

    /**
     * An event signals an opportunity to release a vehicle packet. The lanegroup must,
     * 1. construct packets to be released to each of the lanegroups reached by each of it's
     *    road connections.
     * 2. check what portion of each of these packets will be accepted. Reduce the packets
     *    if necessary.
     * 3. call next_link.add_vehicle_packet for each reduces packet.
     * 4. remove the vehicle packets from this lanegroup.
     */
    abstract public void release_vehicle_packets(float timestamp) throws OTMException;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public AbstractLaneGroup(Link link, Side side, FlowDirection flwdir, float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs){
        this.link = link;
        this.side = side;
        this.flwdir = flwdir;
        this.length = length;
        this.num_lanes = num_lanes;
        this.id = OTMUtils.get_lanegroup_id();
        this.states = new HashSet<>();
        switch(flwdir){
            case up:
                this.start_lane_up = start_lane;
                break;
            case dn:
                this.start_lane_dn = start_lane;
                break;
        }
        this.outlink2roadconnection = new HashMap<>();
        this.state2roadconnection = new HashMap<>();
        if(out_rcs!=null)
            for(RoadConnection rc : out_rcs)
                if(rc.has_end_link())
                    outlink2roadconnection.put(rc.get_end_link_id(),rc);
    }

    public void delete(){
        link = null;
        actuator = null;
        flw_acc = null;
        outlink2roadconnection = null;
    }

    public void validate(OTMErrorLog errorLog) {
        // out_road_connections all lead to links that are immediately downstream
        Set dwn_links = link.end_node.out_links.values().stream().map(x->x.getId()).collect(Collectors.toSet());
        if(!dwn_links.containsAll(outlink2roadconnection.keySet()))
            errorLog.addError("some outlinks are not immediately downstream");

    }

    public void initialize(Scenario scenario, RunParameters runParams) throws OTMException {
        if(flw_acc!=null)
            flw_acc.reset();
    }

    public void set_road_params(jaxb.Roadparam r){
        // all lanes in the lanegroup are expected to have the same length
        max_vehicles = r.getJamDensity()*length*num_lanes/1000f;
    }

    public FlowAccumulatorState request_flow_accumulator(KeyCommPathOrLink key){
        if(flw_acc==null)
            flw_acc = new FlowAccumulatorState();
        flw_acc.add_key(key);
        return flw_acc;
    }

    public FlowAccumulatorState request_flow_accumulator(Long comm_id){
        if(flw_acc==null)
            flw_acc = new FlowAccumulatorState();
        for(KeyCommPathOrLink key : states)
            if(key.commodity_id==comm_id)
                flw_acc.add_key(key);
        return flw_acc;
    }

    public FlowAccumulatorState request_flow_accumulator(){
        if(flw_acc==null)
            flw_acc = new FlowAccumulatorState();
        for(KeyCommPathOrLink key : states)
            flw_acc.add_key(key);
        return flw_acc;
    }

    public void add_state(long comm_id, Long path_id,Long next_link_id, boolean ispathfull) throws OTMException {

        KeyCommPathOrLink state = ispathfull ?
                new KeyCommPathOrLink(comm_id, path_id, true) :
                new KeyCommPathOrLink(comm_id, next_link_id, false);

        states.add(state);

        if(link.is_sink){
            state2roadconnection.put(state,null);
            state2lanechangedirection.put(state, Side.stay);
        } else {

            // store in map
            RoadConnection rc = outlink2roadconnection.get(next_link_id);
            if(rc!=null) {

                // state2roadconnection
                state2roadconnection.put(state, rc.getId());

                // state2lanechangedirection
                Set<AbstractLaneGroup> target_lgs = rc.in_lanegroups;
                Set<Side> sides = target_lgs.stream().map(x -> x.get_side_with_respect_to_lg(this)).collect(Collectors.toSet());
                if (sides.size() != 1)
                    throw new OTMException("asd;liqwr g-q4iwq jg");
                state2lanechangedirection.put(state, sides.iterator().next());
            }
        }

    }

    ///////////////////////////////////////////////////
    // get
    ///////////////////////////////////////////////////

    public final float get_total_vehicles() {
        return vehs_dwn_for_comm(null);
    }

    public final double get_space_per_lane() {
        return get_space()/num_lanes;
    }

    public final float get_space() {
        return max_vehicles- vehs_dwn_for_comm(null);
    }

    public final List<Integer> get_dn_lanes(){
        return IntStream.range(start_lane_dn,start_lane_dn+num_lanes).boxed().collect(toList());
    }

    public final List<Integer> get_up_lanes(){
        return IntStream.range(start_lane_up,start_lane_up+num_lanes).boxed().collect(toList());
    }

    public final Side get_side_with_respect_to_lg(AbstractLaneGroup lg){

        // This is more complicated with up addlanes
        assert(lg.flwdir==FlowDirection.dn);
        assert(this.flwdir==FlowDirection.dn);

        if(this.link.getId()!=lg.link.getId())
            return null;

        if(this==lg)
            return Side.stay;

        if (this.start_lane_dn < lg.start_lane_dn)
            return Side.in;
        else
            return Side.out;
    }

    public Set<Long> get_dwn_links(){
        return outlink2roadconnection.keySet();
    }

    public boolean link_is_link_reachable(Long link_id){
        return outlink2roadconnection.containsKey(link_id);
    }


//    public Set<AbstractLaneGroup> get_accessible_lgs_in_outlink(Link out_link){
//
//        // if the end node is one to one, then all lanegroups in the next link are equally accessible
//        if(link.end_node.is_many2one) {
//            if (link.outlink2lanegroups.containsKey(out_link.getId()))
//                return new HashSet<>(out_link.lanegroups_flwdn.values());     // all downstream lanegroups are accessible
//            else
//                return null;
//        }
//
//        // otherwise, get the road connection connecting this lg to out_link
//        RoadConnection rc = outlink2roadconnection.get(out_link.getId());
//
//        // return lanegroups connected to by this road connection
//        return out_link.get_unique_lanegroups_for_up_lanes(rc.end_link_from_lane,rc.end_link_to_lane);
//
//    }

    ///////////////////////////////////////////////////
    // update
    ///////////////////////////////////////////////////

    protected void update_flow_accummulators(KeyCommPathOrLink key,double num_vehicles){
        if(flw_acc!=null)
            flw_acc.increment(key,num_vehicles);
    }

    ///////////////////////////////////////////////////
    // other
    ///////////////////////////////////////////////////

    public RoadConnection get_target_road_connection_for_state(KeyCommPathOrLink key){
        Long outlink_id = key.isPath ? link.path2outlink.get(key.pathOrlink_id).getId() : key.pathOrlink_id;
        return outlink2roadconnection.get(outlink_id);
    }

    @Override
    public String toString() {
        return String.format("link %d, lg %d, lanes %d, start_dn %d, start_up %d",link.getId(),id,num_lanes,start_lane_dn,start_lane_up);
    }

    @Override
    public int compareTo(AbstractLaneGroup that) {

        System.err.println("WPOGJOWRGPOIWEGJPOIWEJGPOIJWEGPOI");
        return 0;

//        int this_start = this.lanes.stream().min(Integer::compareTo).get();
//        int that_start = that.lanes.stream().min(Integer::compareTo).get();
//        if(this_start < that_start)
//            return -1;
//        if(that_start < this_start)
//            return 1;
//
//        int this_end = this.lanes.stream().max(Integer::compareTo).get();
//        int that_end = that.lanes.stream().max(Integer::compareTo).get();
//        if(this_end < that_end)
//            return -1;
//        if(that_end < this_end)
//            return 1;
//
//        return 0;
    }

}
