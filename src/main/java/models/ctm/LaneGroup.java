/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.ctm;

import commodity.Commodity;
import commodity.Path;
import common.*;
import common.RoadConnection;
import error.OTMErrorLog;
import error.OTMException;
import keys.KeyCommPathOrLink;
import packet.AbstractPacketLaneGroup;
import runner.RunParameters;
import runner.Scenario;
import utils.OTMUtils;

import java.util.*;

public class LaneGroup extends AbstractLaneGroup {

    public double cell_length_meters;

    // set of keys for states in this lanegroup
    public Set<KeyCommPathOrLink> states;

    // exiting road connection to the states that use it (should be avoided in the one-to-one case)
    public Map<Long,Set<KeyCommPathOrLink>> roadconnection2states;

    // state to the road connection it must use (should be avoided in the one-to-one case)
    public Map<KeyCommPathOrLink,Long> state2roadconnection;

    public List<Cell> cells;     // sequence of cells

    // transversal flow of vehicles already in their target lanegroup
    public List<Map<KeyCommPathOrLink,Double>> flow_in_target;

    // transversal flow of vehicles not in their target lanegroup
    public List<Map<KeyCommPathOrLink,Double>> flow_notin_target;

    ////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public LaneGroup(Link link, Set<Integer> lanes, Set<RoadConnection> out_rcs){
        super(link, lanes, out_rcs);

        states = new HashSet<>();
        state2roadconnection = new HashMap<>();
    }

    protected void create_cells(int num_cells,double cell_length_meters){

        this.cells = new ArrayList<>();

        this.cell_length_meters = cell_length_meters;

        for(int i=0;i<num_cells;i++)
            this.cells.add(new Cell((models.ctm.LinkModel)link.model, cell_length_meters, this));

        // designate first and last
        this.cells.get(0).am_upstrm = true;
        this.cells.get(num_cells-1).am_dnstrm = true;

//        // populate boundary flows
//        flow_in_target = new ArrayList<>();
//        for(int i=0;i<num_cells+1;i++)
//            flow_in_target.add(new HashMap<>());

    }

    public void add_key(KeyCommPathOrLink state) throws OTMException {

        states.add(state);

        // state2roadconnection: for this state, what is the road connection exiting
        // this lanegroup that it will follow. There need not be one: this may not be
        // a target lane group for this state.

        // sink case -- no road connection
        if(link.is_sink){
            state2roadconnection.put(state,null);
            return;
        }

        // get next link according to the case
        Long next_link;
        if(link.end_node.is_many2one){
            next_link = link.end_node.out_links.values().iterator().next().getId();
        }
        else {
            if (state.isPath) {
                Path path = (Path) link.network.scenario.subnetworks.get(state.pathOrlink_id);
                next_link = path.get_link_following(link).getId();
            } else {
                next_link = state.pathOrlink_id;
            }
        }

        // store in map
        RoadConnection rc = get_roadconnection_for_outlink(next_link);
        if(rc!=null)
            state2roadconnection.put(state,rc.getId());

    }

    public void allocate_state(){

        // allocate for each cell
        cells.forEach(c -> c.allocate_state());

        // initialize roadconnection2states
        roadconnection2states = new HashMap<>();
        for(common.RoadConnection rc : outlink2roadconnection.values())
            roadconnection2states.put(rc.getId(),new HashSet<>());

        // add all states
        for (KeyCommPathOrLink key : states) {
            Long outlink_id = key.isPath ? link.path2outlink.get(key.pathOrlink_id) :
                                           key.pathOrlink_id;

            common.RoadConnection rc = get_roadconnection_for_outlink(outlink_id);
            if (rc!=null && roadconnection2states.containsKey(rc.getId()))
                roadconnection2states.get(rc.getId()).add(key);
        }


    }

    ////////////////////////////////////////////
    // implementation
    ////////////////////////////////////////////

    @Override
    public void add_commodity(Commodity commodity) {
        // currently doing nothing here
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);
        cells.forEach(c->c.validate(errorLog));
    }

    @Override
    public void initialize(Scenario scenario, RunParameters runParams) throws OTMException {
        super.initialize(scenario,runParams);

        // populate in target boundary flows
        flow_in_target = new ArrayList<>();
        for(int i=0;i<cells.size()+1;i++)
            flow_in_target.add(new HashMap<>());

        // Additional configuration for lane changing
        Set<AbstractLaneGroup> my_neighbors = get_my_neighbors();
        if(my_neighbors!=null){

            // populate not in target boundary flows
            flow_notin_target = new ArrayList<>();
            for(int i=0;i<cells.size()+1;i++)
                flow_notin_target.add(new HashMap<>());

            // tell cells who are their neighbors
            models.ctm.LaneGroup neighbor_lg = (models.ctm.LaneGroup) my_neighbors.iterator().next();
            for(int i=0;i<cells.size();i++)
                cells.get(i).neighbor = neighbor_lg.cells.get(i);
        }

    }

    @Override
    public void add_native_vehicle_packet(float timestamp, AbstractPacketLaneGroup avp) {

        models.ctm.PacketLaneGroup vp = (models.ctm.PacketLaneGroup) avp;

        // case sink or the packet is targeted for this lanegroup
        if(vp.target_lanegroups==null || vp.target_lanegroups.contains(this))
            copy_to_flow(vp,flow_in_target);
        else    // case the packet is targeted for some other lanegroup
            copy_to_flow(vp,flow_notin_target);
    }

    @Override
    public void exiting_roadconnection_capacity_has_been_modified(float timestamp) {
        // do nothing
    }

    @Override
    public void release_vehicle_packets(float timestamp) throws OTMException {
        throw new OTMException("This should not be called.");
    }

    @Override
    public float get_current_travel_time() {

        double travel_time;
        double sim_dt = link.network.scenario.sim_dt;
        float sum = 0f;
        for(int i=0;i<cells.size();i++){
            Cell cell = cells.get(i);

            double veh = cell.get_vehicles();   // [veh]

            if(veh>0) {

                Map<KeyCommPathOrLink,Double> bf = flow_in_target.get(i+1);
                double out_flow = bf==null ? 0d : bf.values().stream().mapToDouble(x->x).sum();

                if(out_flow==0)
                    travel_time = link.is_source ? sim_dt : sim_dt / cell.ffspeed_norm;
                else
                    travel_time = sim_dt * veh / out_flow;

            } else
                travel_time = link.is_source ? sim_dt : sim_dt / cell.ffspeed_norm;

            sum += travel_time;
        }
        return sum;
    }

    @Override
    public float vehicles_for_commodity(Long commodity_id) {
        return (float) cells.stream().mapToDouble(c->c.get_vehicles_for_commodity(commodity_id)).sum();
    }

    @Override
    public double get_supply(){
        return get_upstream_cell().supply;
    }

    ////////////////////////////////////////////
    // ctm update
    ////////////////////////////////////////////

    // not called for sinks
    public void release_vehicles(Map<KeyCommPathOrLink,Double> X){
        flow_in_target.set(cells.size(),X);
    }

    protected void update_cell_boundary_flows(){
        // set flow_in_target and flot_notin_target for internal boundaries and
        // downstream boundary for sinks

        if(states.isEmpty())
            return;

        // make a list of flow maps
        // there are (#cells)+1 boundaries
        // each map is from comm_path to value
        for(int i=0;i<cells.size()-1;i++){
            Map<KeyCommPathOrLink,Double> demand_in_target = cells.get(i).demand_in_target;
            Map<KeyCommPathOrLink,Double> demand_notin_target = cells.get(i).demand_notin_target;

            double total_demand = OTMUtils.sum(demand_in_target);
            total_demand += demand_notin_target==null ? 0d : OTMUtils.sum(demand_notin_target);

            if(total_demand>OTMUtils.epsilon) {
                double total_flow = Math.min( total_demand , cells.get(i+1).supply );
                double gamma = total_flow / total_demand;
                flow_in_target.set(i+1,OTMUtils.times(demand_in_target,gamma));

                if(flow_notin_target!=null)
                    flow_notin_target.set(i+1,OTMUtils.times(demand_notin_target,gamma));
            }
            else {
                flow_in_target.set(i+1, null);
                if(flow_notin_target!=null)
                    flow_notin_target.set(i+1, null);
            }
        }

        if(link.end_node.is_sink) {
            flow_in_target.set(cells.size(), cells.get(cells.size()-1).demand_in_target);
            if(flow_notin_target!=null)
                flow_notin_target.set(cells.size(), cells.get(cells.size()-1).demand_notin_target);
        }

        // send lanegroup exit flow to flow accumulator
        // TODO : Check this
        for(Map.Entry<KeyCommPathOrLink,Double> e : flow_in_target.get(cells.size()).entrySet())
            if(e.getValue()>0)
                update_flow_accummulators(e.getKey(),e.getValue());
    }

    protected void update_state(){

        if(states.isEmpty())
            return;

        for(int i=0;i<cells.size();i++) {
            cells.get(i).update_in_target_state(flow_in_target.get(i), flow_in_target.get(i + 1));
            if(flow_notin_target!=null)
                cells.get(i).update_notin_target_state(flow_notin_target.get(i), flow_notin_target.get(i + 1));
        }

        // clear boundary flows
        for(int i=link.is_source?1:0;i<=cells.size();i++)
            flow_in_target.set(i,null);

        if(flow_notin_target!=null)
            for(int i=0;i<=cells.size();i++)
                flow_notin_target.set(i,null);
    }

    ////////////////////////////////////////////
    // public
    ////////////////////////////////////////////

    public int get_num_exiting_road_connections(){
        return link.end_node.is_many2one ? 0 : roadconnection2states.size();
    }

    public double get_total_in_flow(){
        Map<KeyCommPathOrLink,Double> bf = flow_in_target.get(0);
        return bf==null ? 0d : bf.values().stream().mapToDouble(x->x).sum();
    }

    public double get_total_out_flow(){
        Map<KeyCommPathOrLink,Double> bf = flow_in_target.get(flow_in_target.size()-1);
        return bf==null ? 0d : bf.values().stream().mapToDouble(x->x).sum();
    }

    public Cell get_upstream_cell(){
        return cells.get(0);

    }

    public Cell get_dnstream_cell(){
        return cells.get(cells.size()-1);
    }

    public Double get_demand_in_target_for_comm_pathORlink(KeyCommPathOrLink comm_pathOrLink){
        return get_dnstream_cell().demand_in_target.get(comm_pathOrLink);
    }

    public String print_cell_veh(){
        String str = "| ";
        for(Cell cell : cells)
            str += String.format("(%5.1f,%5.1f)",cell.get_vehicles_in_target(),cell.get_vehicles_notin_target()) + " | ";
        return str;
    }

    ////////////////////////////////////////////
    // private
    ///////////////////////////////////////////

    private void copy_to_flow(models.ctm.PacketLaneGroup vp,List<Map<KeyCommPathOrLink,Double>> flw){
        Map<KeyCommPathOrLink,Double> bf = flw.get(0);
        if(bf==null)
            flw.set(0,vp.state2vehicles);
        else {
            for(Map.Entry<KeyCommPathOrLink,Double> e : vp.state2vehicles.entrySet())
                bf.put(e.getKey(), bf.containsKey(e.getKey()) ?  bf.get(e.getKey()) + e.getValue() : e.getValue() );
//            flw.set(0,bf);
        }
//        flw.set(0,vp.state2vehicles);
    }


}