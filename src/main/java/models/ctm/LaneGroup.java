/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.ctm;

import commodity.Commodity;
import common.*;
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

public class LaneGroup extends AbstractLaneGroup {

    public double cell_length_meters;

    public List<Cell> cells;     // sequence of cells

    // transversal flow of vehicles already in their target lanegroup
    // size = numcells + 1
    public List<Map<KeyCommPathOrLink,Double>> flow_dwn;

    // transversal flow of vehicles not in their target lanegroup
    // size = numcells (downstream boundary is always 0)
    public List<Map<KeyCommPathOrLink,Double>> flow_in;
    public List<Map<KeyCommPathOrLink,Double>> flow_out;

    ////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public LaneGroup(Link link, Side side, FlowDirection flwdir, float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs) {
        super(link, side, flwdir,length, num_lanes, start_lane, out_rcs);
    }

    protected void create_cells(int num_cells,double cell_length_meters){

        this.cells = new ArrayList<>();

        this.cell_length_meters = cell_length_meters;

        for(int i=0;i<num_cells;i++)
            this.cells.add(new Cell((models.ctm.LinkModel)link.model, cell_length_meters, this));

        // designate first and last
        this.cells.get(0).am_upstrm = true;
        this.cells.get(num_cells-1).am_dnstrm = true;

    }

    @Override
    public void allocate_state() {
        super.allocate_state();
        cells.forEach(c -> c.allocate_state());
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
        flow_dwn = new ArrayList<>();
        for(int i=0;i<cells.size()+1;i++)
            flow_dwn.add(new HashMap<>());

        // Configuration for lane changing
        if(neighbor_in!=null){
            this.flow_in = new ArrayList<>();
            for(int i=0;i<cells.size();i++)
                flow_in.add(new HashMap<>());
        }

        if(neighbor_out!=null){
            this.flow_out = new ArrayList<>();
            for(int i=0;i<cells.size()+1;i++)
                flow_out.add(new HashMap<>());
        }

    }

    @Override
    public void add_native_vehicle_packet(float timestamp, AbstractPacketLaneGroup avp) {

        models.ctm.PacketLaneGroup vp = (models.ctm.PacketLaneGroup) avp;

        Map<KeyCommPathOrLink,Double> bf_dwn = flow_dwn.get(0);
        Map<KeyCommPathOrLink,Double> bf_in = flow_in==null ? null : flow_in.get(0);
        Map<KeyCommPathOrLink,Double> bf_out = flow_out==null ? null : flow_out.get(0);

        for(Map.Entry<KeyCommPathOrLink,Double> e : vp.state2vehicles.entrySet()) {
            KeyCommPathOrLink state = e.getKey();
            Double val = e.getValue();

            if(vp.target_lanegroups==null || vp.target_lanegroups.contains(this)){
                if(bf_dwn==null) {
                    bf_dwn = new HashMap<>();
                    bf_dwn.put(state, val);
                }
                else
                    bf_dwn.put(state, bf_dwn.containsKey(state) ? bf_dwn.get(state)+val : val);
                continue;
            }

            Side flw_direction = this.state2lanechangedirection.get(state);
            switch(flw_direction){
                case in:
                    if(bf_in==null) {
                        bf_in = new HashMap<>();
                        bf_in.put(state, val);
                    }
                    else
                        bf_in.put(state, bf_in.containsKey(state) ? bf_in.get(state)+val : val);
                    break;
                case out:
                    if(bf_out==null) {
                        bf_out = new HashMap<>();
                        bf_out.put(state, val);
                    }
                    else
                        bf_out.put(state, bf_out.containsKey(state) ? bf_out.get(state)+val : val);
                    break;
                case full:
                    if(bf_dwn==null) {
                        bf_dwn = new HashMap<>();
                        bf_dwn.put(state, val);
                    }
                    else
                        bf_dwn.put(state, bf_dwn.containsKey(state) ? bf_dwn.get(state)+val : val);
                    break;
            }

        }

        if(bf_dwn!=null)
            flow_dwn.set(0,bf_dwn);

        if(bf_in!=null)
            flow_in.set(0,bf_in);

        if(bf_out!=null)
            flow_out.set(0,bf_out);
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

                Map<KeyCommPathOrLink,Double> bf = flow_dwn.get(i+1);
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
    public float vehs_dwn_for_comm(Long comm_id) {
        return (float) cells.stream().mapToDouble(c->c.get_veh_dwn_for_commodity(comm_id)).sum();
    }

    @Override
    public float vehs_in_for_comm(Long comm_id) {
        return (float) cells.stream().mapToDouble(c->c.get_veh_in_for_commodity(comm_id)).sum();
    }

    @Override
    public float vehs_out_for_comm(Long comm_id) {
        return (float) cells.stream().mapToDouble(c->c.get_veh_out_for_commodity(comm_id)).sum();
    }

    @Override
    public double get_supply(){
        Cell upcell = get_upstream_cell();
        return Double.isInfinite(upcell.jam_density_veh) ?
                Double.POSITIVE_INFINITY :
                upcell.wspeed_norm * (upcell.jam_density_veh - upcell.get_vehicles());
    }

    @Override
    public void set_max_speed_mps(Float max_speed_mps) throws OTMException {
        throw new OTMException("NOT IMPLEMENTED");
    }

    @Override
    public void set_max_flow_vpspl(Float max_flow_vpspl) throws OTMException {
        throw new OTMException("NOT IMPLEMENTED");
    }

    @Override
    public void set_max_density_vpkpl(Float max_density_vpkpl) throws OTMException {
        throw new OTMException("NOT IMPLEMENTED");
    }

    ////////////////////////////////////////////
    // update
    ////////////////////////////////////////////

    protected void update_dwn_flow(){
        // set flow_dwn and flot_notin_target for internal boundaries and
        // downstream boundary for sinks

        if(states.isEmpty())
            return;

        // make a list of flow maps
        // there are (#cells)+1 boundaries
        // each map is from comm_path to value
        for(int i=0;i<cells.size()-1;i++){

            Cell cell = cells.get(i);

            Map<KeyCommPathOrLink,Double> dem_dwn = cell.demand_dwn;
            Map<KeyCommPathOrLink,Double> dem_out = cell.demand_out;
            Map<KeyCommPathOrLink,Double> dem_in = cell.demand_in;

            // total demand
            double total_demand = OTMUtils.sum(dem_dwn);
            total_demand += dem_out==null ? 0d : OTMUtils.sum(dem_out);
            total_demand += dem_in==null ? 0d : OTMUtils.sum(dem_in);

            if(total_demand>OTMUtils.epsilon) {
                double total_flow = Math.min( total_demand , cells.get(i+1).supply );
                double gamma = total_flow / total_demand;

                flow_dwn.set(i+1,OTMUtils.times(dem_dwn,gamma));
                if(flow_in!=null)
                    flow_in.set(i+1,OTMUtils.times(dem_in,gamma));

                if(flow_out!=null)
                    flow_out.set(i+1,OTMUtils.times(dem_out,gamma));
            }
            else {
                flow_dwn.set(i+1, null);
                if(flow_in!=null)
                    flow_in.set(i+1, null);
                if(flow_out!=null)
                    flow_out.set(i+1, null);
            }
        }

        if(link.end_node.is_sink)
            flow_dwn.set(cells.size(), cells.get(cells.size()-1).demand_dwn);

        // send lanegroup exit flow to flow accumulator
        // TODO : NOT IN TARGET VEHICLES ARE NOT BEING COUNTED!!!
        for(Map.Entry<KeyCommPathOrLink,Double> e : flow_dwn.get(cells.size()).entrySet())
            if(e.getValue()>0)
                update_flow_accummulators(e.getKey(),e.getValue());
    }

    protected void update_state(float timestamp){

        if(states.isEmpty())
            return;

        for(int i=0;i<cells.size();i++) {
            Cell cell = cells.get(i);
            cell.update_dwn_state(flow_dwn.get(i), flow_dwn.get(i + 1));
            if(flow_in!=null)
                cell.update_in_state(flow_in.get(i), i==cells.size()-1 ? null : flow_in.get(i+1));
            if(flow_out!=null)
                cell.update_out_state(flow_out.get(i), i==cells.size()-1 ? null : flow_out.get(i+1));
        }

        // clear boundary flows
        // TODO: IS THIS A GOOD IDEA?
        for(int i=link.is_source?1:0;i<=cells.size();i++)
            flow_dwn.set(i,null);

        if(flow_in!=null)
            for(int i=0;i<cells.size();i++)
                flow_in.set(i,null);
        if(flow_out!=null)
            for(int i=0;i<cells.size();i++)
                flow_out.set(i,null);
    }

    // not called for sinks
    public void release_vehicles(Map<KeyCommPathOrLink,Double> X){
        flow_dwn.set(cells.size(),X);
    }

    ////////////////////////////////////////////
    // get
    ////////////////////////////////////////////

    public Cell get_upstream_cell(){
        return cells.get(0);
    }

    public Cell get_dnstream_cell(){
        return cells.get(cells.size()-1);
    }

    public Double get_demand_in_target_for_state(KeyCommPathOrLink state){
        return get_dnstream_cell().demand_dwn.get(state);
    }

    public double get_total_outgoing_flow(){
        return flow_dwn==null || flow_dwn.size()<cells.size()+1 || flow_dwn.get(cells.size())==null ? 0d : OTMUtils.sum(flow_dwn.get(cells.size()));
    }

    public double get_total_incoming_flow(){
        double flow = 0d;
        if(flow_dwn!=null && flow_dwn.get(0)!=null)
            flow += OTMUtils.sum(flow_dwn.get(0));
        if(flow_in!=null && flow_in.get(0)!=null)
            flow += OTMUtils.sum(flow_in.get(0));
        if(flow_out!=null && flow_out.get(0)!=null)
            flow += OTMUtils.sum(flow_out.get(0));
        return flow;
    }

}