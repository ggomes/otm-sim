/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.ctm;

import common.*;
import common.RoadConnection;
import error.OTMErrorLog;
import error.OTMException;
import geometry.FlowDirection;
import geometry.Side;
import keys.KeyCommPathOrLink;
import models.AbstractLaneGroup;
import packet.FluidLaneGroupPacket;
import packet.AbstractPacketLaneGroup;
import runner.RunParameters;
import runner.Scenario;
import utils.OTMUtils;

import java.util.*;

public class LaneGroup extends AbstractLaneGroup {

    public double cell_length_meters;

    public double wspeed_cell_per_dt;          // [-]
    public double ffspeed_cell_per_dt;         // [-]
    public double jam_density_veh_per_cell;
    public double capacity_veh_per_dt;

    public List<Cell> cells;     // sequence of cells

    // transversal flow of vehicles already in their target lanegroup
    // size = numcells + 1
    public List<Map<KeyCommPathOrLink,Double>> flow_stay;

    // flow of vehicles changing lanes in the inner and outer directions
    // size = numcells (downstream boundary is always 0)
    public List<Map<KeyCommPathOrLink,Double>> flow_lc_in;
    public List<Map<KeyCommPathOrLink,Double>> flow_lc_out;

    ////////////////////////////////////////////
    // load
    ///////////////////////////////////////////

    public LaneGroup(Link link, Side side, FlowDirection flwdir, float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs) {
        super(link, side, flwdir,length, num_lanes, start_lane, out_rcs);
    }

    public void create_cells(int num_cells,double cell_length_meters){

        this.cells = new ArrayList<>();

        this.cell_length_meters = cell_length_meters;

        for(int i=0;i<num_cells;i++)
            this.cells.add(new Cell(cell_length_meters, this));

        // designate first and last
        this.cells.get(0).am_upstrm = true;
        this.cells.get(num_cells-1).am_dnstrm = true;

    }

    @Override
    public void allocate_state() {
        cells.forEach(c -> c.allocate_state());
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);

        if (jam_density_veh_per_cell < 0)
            errorLog.addError("non-negativity");

        if (!link.is_source) {
            if (ffspeed_cell_per_dt < 0)
                errorLog.addError("non-negativity");
            if (wspeed_cell_per_dt < 0)
                errorLog.addError("non-negativity");
            if (wspeed_cell_per_dt > 1)
                errorLog.addError("CFL violated: link " + link.getId() + " wspeed_cell_per_dt = " + wspeed_cell_per_dt);
            if (ffspeed_cell_per_dt > 1)
                errorLog.addError("CFL violated: link " + link.getId() + " ffspeed_cell_per_dt = " + ffspeed_cell_per_dt);
        }

    }

    @Override
    public void initialize(Scenario scenario, RunParameters runParams) throws OTMException {
        super.initialize(scenario,runParams);

        // populate in target boundary flows
        flow_stay = new ArrayList<>();
        for(int i=0;i<cells.size()+1;i++)
            flow_stay.add(new HashMap<>());

        // Configuration for lane changing
        if(neighbor_in!=null){
            this.flow_lc_in = new ArrayList<>();
            for(int i=0;i<cells.size();i++)
                flow_lc_in.add(new HashMap<>());
        }

        if(neighbor_out!=null){
            this.flow_lc_out = new ArrayList<>();
            for(int i=0;i<cells.size()+1;i++)
                flow_lc_out.add(new HashMap<>());
        }

    }

    @Override
    public void exiting_roadconnection_capacity_has_been_modified(float timestamp) {
        // do nothing
    }

    ////////////////////////////////////////////
    // run
    ////////////////////////////////////////////

    @Override
    public void add_vehicle_packet(float timestamp, AbstractPacketLaneGroup avp, Long nextlink_id) {

        FluidLaneGroupPacket vp = (FluidLaneGroupPacket) avp;

        Map<KeyCommPathOrLink,Double> bf_dwn = flow_stay.get(0);
        Map<KeyCommPathOrLink,Double> bf_in = flow_lc_in ==null ? null : flow_lc_in.get(0);
        Map<KeyCommPathOrLink,Double> bf_out = flow_lc_out ==null ? null : flow_lc_out.get(0);

        for(Map.Entry<KeyCommPathOrLink,Double> e : vp.state2vehicles.entrySet()) {
            KeyCommPathOrLink state = e.getKey();
            Double val = e.getValue();

            switch(state2lanechangedirection.get(state)){
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
                case stay:
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
            flow_stay.set(0,bf_dwn);

        if(bf_in!=null)
            flow_lc_in.set(0,bf_in);

        if(bf_out!=null)
            flow_lc_out.set(0,bf_out);

        update_supply();

    }

    @Override
    public void release_vehicle_packets(float timestamp) throws OTMException {
        throw new OTMException("This should not be called.");
    }

    // not called for sinks
    public void release_vehicles(Map<KeyCommPathOrLink,Double> X){
        flow_stay.set(cells.size(),X);
    }

    protected void update_dwn_flow(){
        // set flow_stay and flot_notin_target for internal boundaries and
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

                flow_stay.set(i+1,OTMUtils.times(dem_dwn,gamma));
                if(flow_lc_in !=null)
                    flow_lc_in.set(i+1,OTMUtils.times(dem_in,gamma));

                if(flow_lc_out !=null)
                    flow_lc_out.set(i+1,OTMUtils.times(dem_out,gamma));
            }
            else {
                flow_stay.set(i+1, null);
                if(flow_lc_in !=null)
                    flow_lc_in.set(i+1, null);
                if(flow_lc_out !=null)
                    flow_lc_out.set(i+1, null);
            }
        }

        if(link.end_node.is_sink)
            flow_stay.set(cells.size(), cells.get(cells.size()-1).demand_dwn);

        // send lanegroup exit flow to flow accumulator
        // TODO : NOT IN TARGET VEHICLES ARE NOT BEING COUNTED!!!
        for(Map.Entry<KeyCommPathOrLink,Double> e : flow_stay.get(cells.size()).entrySet())
            if(e.getValue()>0)
                update_flow_accummulators(e.getKey(),e.getValue());
    }

    protected void update_state(float timestamp){

        if(states.isEmpty())
            return;

        for(int i=0;i<cells.size();i++) {
            Cell cell = cells.get(i);
            cell.update_dwn_state(flow_stay.get(i), flow_stay.get(i + 1));
            if(flow_lc_in !=null)
                cell.update_in_state(flow_lc_in.get(i), i==cells.size()-1 ? null : flow_lc_in.get(i+1));
            if(flow_lc_out !=null)
                cell.update_out_state(flow_lc_out.get(i), i==cells.size()-1 ? null : flow_lc_out.get(i+1));
        }

        // clear boundary flows
        // TODO: IS THIS A GOOD IDEA?
        for(int i=link.is_source?1:0;i<=cells.size();i++)
            flow_stay.set(i,null);

        if(flow_lc_in !=null)
            for(int i=0;i<cells.size();i++)
                flow_lc_in.set(i,null);
        if(flow_lc_out !=null)
            for(int i=0;i<cells.size();i++)
                flow_lc_out.set(i,null);
    }

    @Override
    public void update_supply(){
        Cell upcell = get_upstream_cell();
        upcell.update_supply();
        supply = upcell.supply;
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

//    public double get_total_outgoing_flow(){
//        return flow_stay ==null || flow_stay.size()<cells.size()+1 || flow_stay.get(cells.size())==null ? 0d : OTMUtils.sum(flow_stay.get(cells.size()));
//    }
//
//    public double get_total_incoming_flow(){
//        double flow = 0d;
//        if(flow_stay !=null && flow_stay.get(0)!=null)
//            flow += OTMUtils.sum(flow_stay.get(0));
//        if(flow_lc_in !=null && flow_lc_in.get(0)!=null)
//            flow += OTMUtils.sum(flow_lc_in.get(0));
//        if(flow_lc_out !=null && flow_lc_out.get(0)!=null)
//            flow += OTMUtils.sum(flow_lc_out.get(0));
//        return flow;
//    }

    @Override
    public float get_current_travel_time() {

        double travel_time;
        double sim_dt = ((models.ctm.Model_CTM)link.model).dt;
        float sum = 0f;
        for(int i=0;i<cells.size();i++){
            Cell cell = cells.get(i);

            double veh = cell.get_vehicles();   // [veh]

            if(veh>0) {

                Map<KeyCommPathOrLink,Double> bf = flow_stay.get(i+1);
                double out_flow = bf==null ? 0d : bf.values().stream().mapToDouble(x->x).sum();

                if(out_flow==0)
                    travel_time = link.is_source ? sim_dt : sim_dt / ffspeed_cell_per_dt;
                else
                    travel_time = sim_dt * veh / out_flow;

            } else
                travel_time = link.is_source ? sim_dt : sim_dt / ffspeed_cell_per_dt;

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

}