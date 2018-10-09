/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.ctm;

import common.*;
import error.OTMErrorLog;
import geometry.Side;
import keys.KeyCommPathOrLink;
import utils.OTMUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LinkModel extends AbstractLinkModel {

    public float ff_travel_time_sec;       // free flow travel time in seconds
    private float cell_length_meters;
    private float capacity_vps;
    private int cells_per_lanegroup;

    ////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public LinkModel(common.Link link){
        super(link);
        myPacketClass = models.ctm.PacketLaneGroup.class;
    }

    ////////////////////////////////////////////
    // public
    ///////////////////////////////////////////

    public void create_cells(float max_cell_length){

        // construct cells
        float r = link.length/max_cell_length;
        boolean is_source_or_sink = link.is_source || link.is_sink;

        cells_per_lanegroup = is_source_or_sink ?
                1 :
                OTMUtils.approximately_equals(r%1.0,0.0) ? (int) r :  1+((int) r);
        cell_length_meters = is_source_or_sink ?
                link.length :
                link.length/cells_per_lanegroup;

        // create cells
        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values())
            ((LaneGroup)lg).create_cells(cells_per_lanegroup,cell_length_meters);
    }

    ////////////////////////////////////////////
    // implementation
    ///////////////////////////////////////////

    @Override
    public void set_road_param(jaxb.Roadparam r, float sim_dt_sec) {

        if(Float.isNaN(sim_dt_sec))
            return;

        // adjustment for MN model
        if(link.model_type==Link.ModelType.mn)
            r.setJamDensity(Float.POSITIVE_INFINITY);

        this.capacity_vps = r.getCapacity()/3600f;

        // normalize
        float dt_hr = sim_dt_sec/3600f;
        float capacity_vehperlane = r.getCapacity()*dt_hr;
        float jam_density_vehperlane = r.getJamDensity() * cell_length_meters / 1000f;
        float ffspeed_veh = 1000f * r.getSpeed()*dt_hr / cell_length_meters;

        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values()) {
            lg.set_road_params(r);
            ((LaneGroup) lg).cells.forEach(c -> c.set_road_params(capacity_vehperlane, jam_density_vehperlane, ffspeed_veh));
        }

        ff_travel_time_sec = 3.6f * link.length / r.getSpeed();
    }

    @Override
    public void validate(OTMErrorLog errorLog) {

        if(link.road_geom!=null)
            errorLog.addError("CTM can't yet deal with non-trivial road geometries.");

        if(ff_travel_time_sec<=0)
            errorLog.addError("ff_travel_time_sec<=0");
        if(cell_length_meters<=0)
            errorLog.addError("cell_length_meters<=0");
        if(capacity_vps<=0)
            errorLog.addError("capacity_vps<=0");
        if(cells_per_lanegroup<=0)
            errorLog.addError("cells_per_lanegroup<=0");
    }

    @Override
    public void reset() {
        for(AbstractLaneGroup alg : link.lanegroups_flwdn.values()){
            LaneGroup lg = (LaneGroup) alg;
            lg.cells.forEach(x->x.reset());
        }
    }

    @Override
    public float get_ff_travel_time() {
        return ff_travel_time_sec;
    }

    @Override
    public float get_capacity_vps() {
        return capacity_vps;
    }

    @Override
    public Map<AbstractLaneGroup,Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups) {
        Map<AbstractLaneGroup,Double> A = new HashMap<>();
        double total_supply = candidate_lanegroups.stream().mapToDouble(x->x.get_supply()).sum();
        for(AbstractLaneGroup laneGroup : candidate_lanegroups)
            A.put(laneGroup , laneGroup.get_supply() / total_supply);
        return A;
    }

    ////////////////////////////////////////////
    // update
    ///////////////////////////////////////////

    public void perform_lane_changes() {

        // scan cross section from upstream to downstream
        for (int i = 0; i < cells_per_lanegroup; i++) {

            Map<Long, Double> gamma = new HashMap<>();

            // compute total flows reduction (gamma)
            for (AbstractLaneGroup lg : link.lanegroups_flwdn.values()) {
                Cell cell = ((LaneGroup) lg).cells.get(i);
                double demand = 0d;
                if (lg.neighbor_in != null)
                    demand += ((LaneGroup) lg.neighbor_in).cells.get(i).total_vehs_out;
                if (lg.neighbor_out != null)
                    demand += ((LaneGroup) lg.neighbor_out).cells.get(i).total_vehs_in;
                double supply = 0.5d * (1d - cell.wspeed_norm) * (cell.jam_density_veh - cell.get_vehicles());
                gamma.put(lg.id, demand > supply ? supply / demand : 1d);
            }

            // lane changes
            for (AbstractLaneGroup lg : link.lanegroups_flwdn.values()) {
                Cell cell = ((LaneGroup) lg).cells.get(i);
                double my_gamma = gamma.get(lg.id);
                if (lg.neighbor_in != null)
                    perform_lane_change(cell.veh_in,(LaneGroup) lg.neighbor_in,i,my_gamma);
                if (lg.neighbor_out != null)
                    perform_lane_change(cell.veh_out,(LaneGroup) lg.neighbor_out,i,my_gamma);
            }
        }

    }

    // call update_supply_demand on each cell
    public void update_supply_demand() {
        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values()) {
            LaneGroup ctmlg = (LaneGroup) lg;
            if(!ctmlg.states.isEmpty())
                ctmlg.cells.forEach(cell -> cell.update_supply_demand());
        }
    }

    public void update_dwn_flow() {
        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values())
            ((LaneGroup) lg).update_dwn_flow();
    }

    public void update_state(float timestamp) {
        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values())
            ((LaneGroup) lg).update_state(timestamp);
    }

//    ////////////////////////////////////////////
//    // get
//    ///////////////////////////////////////////
//
//    public Cell get_upstream_cell_for_lanegroup(Long lg_id){
//        if(link.lanegroups.containsKey(lg_id)) {
//            models.ctm.LaneGroup lg = ((models.ctm.LaneGroup) link.lanegroups.get(lg_id));
//            return lg.cells.get(0);
//        } else {
//            return null;
//        }
//    }
//
//    public Cell get_dnstream_cell_for_lanegroup(Long lg_id){
//        if(link.lanegroups.containsKey(lg_id)) {
//            models.ctm.LaneGroup lg = ((models.ctm.LaneGroup) link.lanegroups.get(lg_id));
//            return lg.cells.get(lg.cells.size()-1);
//        } else {
//            return null;
//        }
//    }
//
//    /** arrange demand into array according to commodity id in clist */
//    public Double get_demand_for_lg_comm_path_rc(Long lg_id, KeyCommodityPathRoadconnection comm_path_rc){
//        Cell dn_cell = get_dnstream_cell_for_lanegroup(lg_id);
//        return dn_cell.demand_dwn.get(comm_path_rc);
//    }
//
//    public Double get_supply_for_lanegroup(Long lg_id){
//        return get_upstream_cell_for_lanegroup(lg_id).supply;
//    }

    private static void perform_lane_change(Map<KeyCommPathOrLink, Double> vehs,LaneGroup neighbor_lg,int i,double gamma){
        for (Map.Entry<KeyCommPathOrLink, Double> e : vehs.entrySet()) {
            Double veh = e.getValue();
            KeyCommPathOrLink state = e.getKey();
            if (veh > 0) {

                double flw = gamma * veh;

                // remove from this cell
                vehs.put(state, vehs.get(state) - flw);

                // add to side cell
                Side newside = neighbor_lg.state2lanechangedirection.get(state);
                Cell neighbor_cell = neighbor_lg.cells.get(i);
                switch (newside) {
                    case in:
                        neighbor_cell.veh_in.put(state, neighbor_cell.veh_in.get(state) + flw);
                        break;
                    case full:
                        neighbor_cell.veh_dwn.put(state, neighbor_cell.veh_dwn.get(state) + flw);
                        break;
                    case out:
                        neighbor_cell.veh_out.put(state, neighbor_cell.veh_out.get(state) + flw);
                        break;
                }
            }
        }

    }

}
