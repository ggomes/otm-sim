/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package common;

import actuator.AbstractActuator;
import commodity.Commodity;
import error.OTMErrorLog;
import error.OTMException;
import geometry.Position;
import geometry.Side;
import keys.KeyCommPathOrLink;
import packet.AbstractPacketLaneGroup;
import runner.RunParameters;
import runner.Scenario;
import sensor.FlowAccumulator;
import utils.OTMUtils;

import java.util.*;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public abstract class AbstractLaneGroup implements Comparable<AbstractLaneGroup> {

    public long id;
    public Link link;
    public Side side;               // inner, full, or outer
    public int start_lane_up;       // counted with respect to upstream boundary
    public int start_lane_dn;       // counted with respect to downstream boundary
    public int num_lanes;

    public AbstractLaneGroup neighbor_in;       // lanegroup down and in
    public AbstractLaneGroup neighbor_out;      // lanegroup down and out
    public AbstractLaneGroup neighbor_up_in;    // lanegroup up and in (full lanes only)
    public AbstractLaneGroup neighbor_up_out;   // lanegroup up and out (full lanes only)

    // set of keys for states in this lanegroup
    public Set<KeyCommPathOrLink> states;

    public float length;

    // parameters
    public float max_vehicles;      // largest number of vehicles that fit in this lane group

    public AbstractActuator actuator;

    // flow accumulator
    public FlowAccumulator flw_acc;

    ///////////////////////////////////////////////////
    // abstract methods
    ///////////////////////////////////////////////////

    abstract public void add_commodity(Commodity commodity);
    abstract public void add_native_vehicle_packet(float timestamp, AbstractPacketLaneGroup vp) throws OTMException;
    abstract public double get_supply();

    /** Return the total number of vehicles in this lane group with the
     * given commodity id. commodity_id==null means return total over all
     * commodities.
     */
    abstract public float vehicles_for_commodity(Long commodity_id);
    abstract public float get_current_travel_time();
    abstract public void allocate_state();
    abstract public void add_key(KeyCommPathOrLink state);

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public AbstractLaneGroup(Link link, Side side, float length, int num_lanes){
        this.link = link;
        this.side = side;
        this.length = length;
        this.num_lanes = num_lanes;
        this.id = OTMUtils.get_lanegroup_id();
        this.states = new HashSet<>();
    }

    public void delete(){
        link = null;
        actuator = null;
        flw_acc = null;
    }

    public void validate(OTMErrorLog errorLog) {
    }

    public void initialize(Scenario scenario, RunParameters runParams) throws OTMException {
        if(flw_acc!=null)
            flw_acc.reset();
    }

    public void set_road_params(jaxb.Roadparam r){
        // all lanes in the lanegroup are expected to have the same length
        max_vehicles = r.getJamDensity()*length*num_lanes/1000f;
    }

    public FlowAccumulator request_flow_accumulator(KeyCommPathOrLink key){
        if(flw_acc==null)
            flw_acc = new FlowAccumulator();
        flw_acc.add_key(key);
        return flw_acc;
    }

    public FlowAccumulator request_flow_accumulator(Long comm_id){
        if(flw_acc==null)
            flw_acc = new FlowAccumulator();
        for(KeyCommPathOrLink key : states)
            if(key.commodity_id==comm_id)
                flw_acc.add_key(key);
        return flw_acc;
    }

    public FlowAccumulator request_flow_accumulator(){
        if(flw_acc==null)
            flw_acc = new FlowAccumulator();
        for(KeyCommPathOrLink key : states)
            flw_acc.add_key(key);
        return flw_acc;
    }

    ///////////////////////////////////////////////////
    // get state
    ///////////////////////////////////////////////////

    public final float get_total_vehicles() {
        return vehicles_for_commodity(null);
    }

    public final double get_space_per_lane() {
        return get_space()/num_lanes;
    }

    public final float get_space() {
        return max_vehicles-vehicles_for_commodity(null);
    }

    public final List<Integer> get_dn_lanes(){
        return IntStream.range(start_lane_dn,start_lane_dn+num_lanes).boxed().collect(toList());
    }

    public final List<Integer> get_up_lanes(){
        return IntStream.range(start_lane_up,start_lane_up+num_lanes).boxed().collect(toList());
    }

    ///////////////////////////////////////////////////
    // set
    ///////////////////////////////////////////////////

    @Override
    public String toString() {
        return String.format("link %d, lg %d, lanes %d, start_dn %d, start_up %d",link.getId(),id,num_lanes,start_lane_dn,start_lane_up);
    }

    ///////////////////////////////////////////////////
    // update
    ///////////////////////////////////////////////////

    protected void update_flow_accummulators(KeyCommPathOrLink key,double num_vehicles){
        if(flw_acc!=null)
            flw_acc.increment(key,num_vehicles);
    }


//    public int distance_to_lanes( int min_lane,int max_lane){
//        int lg_min_lane = lanes.stream().mapToInt(x->x).min().getAsInt();
//        int lg_max_lane = lanes.stream().mapToInt(x->x).max().getAsInt();
//        int distance = Math.max(lg_min_lane-max_lane,min_lane-lg_max_lane);
//        return Math.max(distance,0);
//    }


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
