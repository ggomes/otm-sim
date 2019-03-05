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
import packet.PacketLaneGroup;
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

//        // populate in target boundary flows
//        flow_stay = new ArrayList<>();
//        for(int i=0;i<cells.size()+1;i++)
//            flow_stay.add(new HashMap<>());

//        // Configuration for lane changing
//        if(neighbor_in!=null){
//            this.flow_lc_in = new ArrayList<>();
//            for(int i=0;i<cells.size();i++)
//                flow_lc_in.add(new HashMap<>());
//        }
//
//        if(neighbor_out!=null){
//            this.flow_lc_out = new ArrayList<>();
//            for(int i=0;i<cells.size()+1;i++)
//                flow_lc_out.add(new HashMap<>());
//        }

    }

    @Override
    public void exiting_roadconnection_capacity_has_been_modified(float timestamp) {
        // do nothing
    }

    ////////////////////////////////////////////
    // run
    ////////////////////////////////////////////

    @Override
    public Double get_upstream_vehicle_position(){
        return Double.NaN;
    }

    @Override
    public void add_vehicle_packet(float timestamp, PacketLaneGroup vp, Long nextlink_id) {

        Cell cell = cells.get(0);

        // When the link is a model source, then the packet first goes into a buffer.
        // From there it is "processed", meaning that some part goes into the upstream cell.
        if(link.is_model_source_link) {
            // add packet to buffer
            buffer.add_packet(vp);
            process_buffer(timestamp);
        }

        // otherwise, this is an internal link, and the packet is guaranteed to be
        // purely fluid.
        else {
            for(Map.Entry<KeyCommPathOrLink,Double> e : vp.container.amount.entrySet()) {
                KeyCommPathOrLink key = e.getKey();

                // update state
                if(!key.isPath)
                    key = new KeyCommPathOrLink(key.commodity_id,nextlink_id,false);

                cell.add_vehicles(key,e.getValue());

            }
        }

        update_supply();
    }

    @Override
    public void release_vehicle_packets(float timestamp) throws OTMException {
        throw new OTMException("This should not be called.");
    }

    public void release_vehicles(Map<KeyCommPathOrLink,Double> X){
        cells.get(cells.size()-1).subtract_vehicles(X,null,null);

        // if this is a single cell lane group, then releasing a vehicle will affect the supply
        if(cells.size()==1)
            update_supply();
    }

//    protected void update_dwn_flow(){
//        // set flow_stay and flot_notin_target for internal boundaries and
////        // downstream boundary for sinks
////
////        if(states.isEmpty())
////            return;
////
//        if(link.end_node.is_sink)
//            flow_stay.set(cells.size(), cells.get(cells.size()-1).demand_dwn);
//
//        // send lanegroup exit flow to flow accumulator
//        // TODO : NOT IN TARGET VEHICLES ARE NOT BEING COUNTED!!!
//        for(Map.Entry<KeyCommPathOrLink,Double> e : flow_stay.get(cells.size()).entrySet())
//            if(e.getValue()>0)
//                update_flow_accummulators(e.getKey(),e.getValue());
//    }

//    protected void update_state(float timestamp){
//
//        if(states.isEmpty())
//            return;
//
////        for(int i=0;i<cells.size();i++) {
////            Cell cell = cells.get(i);
////            cell.update_dwn_state(flow_stay.get(i), flow_stay.get(i + 1));
////            if(flow_lc_in !=null)
////                cell.update_in_state(flow_lc_in.get(i), i==cells.size()-1 ? null : flow_lc_in.get(i+1));
////            if(flow_lc_out !=null)
////                cell.update_out_state(flow_lc_out.get(i), i==cells.size()-1 ? null : flow_lc_out.get(i+1));
////        }
//
////        // clear boundary flows
////        // TODO: IS THIS A GOOD IDEA?
////        for(int i=link.is_source?1:0;i<=cells.size();i++)
////            flow_stay.set(i,null);
////
////        if(flow_lc_in !=null)
////            for(int i=0;i<cells.size();i++)
////                flow_lc_in.set(i,null);
////        if(flow_lc_out !=null)
////            for(int i=0;i<cells.size();i++)
////                flow_lc_out.set(i,null);
//    }

    @Override
    public void update_supply(){

        // shut off if buffer is full
        if(link.is_model_source_link && buffer.get_total_veh()>=1d)
            supply = 0d;
        else {
            Cell upcell = get_upstream_cell();
            upcell.update_supply();
            supply = upcell.supply;
        }

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
//        for(int i=0;i<cells.size();i++){
//            Cell cell = cells.get(i);
//
//            double veh = cell.get_vehicles();   // [veh]
//
//            if(veh>0) {
//
//                Map<KeyCommPathOrLink,Double> bf = flow_stay.get(i+1);
//                double out_flow = bf==null ? 0d : bf.values().stream().mapToDouble(x->x).sum();
//
//                if(out_flow==0)
//                    travel_time = link.is_source ? sim_dt : sim_dt / ffspeed_cell_per_dt;
//                else
//                    travel_time = sim_dt * veh / out_flow;
//
//            } else
//                travel_time = link.is_source ? sim_dt : sim_dt / ffspeed_cell_per_dt;
//
//            sum += travel_time;
//        }
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

    protected void process_buffer(float timestamp){
        assert(link.is_model_source_link);

        double buffer_size = buffer.get_total_veh();

        if(buffer_size < OTMUtils.epsilon )
            return;



        Cell cell = cells.get(0);
                    double total_space = cell.supply;
//        double total_space = jam_density_veh_per_cell - cell.get_vehicles();
        double factor = Math.min( 1d , total_space / buffer_size );
        for(Map.Entry<KeyCommPathOrLink,Double> e : buffer.amount.entrySet()) {
            KeyCommPathOrLink key = e.getKey();
            Double buffer_vehs = e.getValue() ;

            // add to cell
            cell.add_vehicles(key,buffer_vehs* factor);

            // remove from buffer
            e.setValue(buffer_vehs*(1d-factor));
        }

    }
}














