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

    public void perform_lane_changes(float timestamp) {
        // WARNING: THIS ASSUMES NO ADDLANES (lanegroups_flwdn=all lanegroups)

        // scan cross section from upstream to downstream
        for (int i = 0; i < cells_per_lanegroup; i++) {

            Map<Long, Double> gamma = new HashMap<>();

            // compute total flows reduction for each lane group
            for (AbstractLaneGroup lg : link.lanegroups_flwdn.values()) {
                Cell cell = ((LaneGroup) lg).cells.get(i);
                double demand_to_me = 0d;
                if (lg.neighbor_in != null)
                    demand_to_me += ((LaneGroup) lg.neighbor_in).cells.get(i).total_vehs_out;
                if (lg.neighbor_out != null)
                    demand_to_me += ((LaneGroup) lg.neighbor_out).cells.get(i).total_vehs_in;
                double supply = 0.9d * (1d - cell.wspeed_norm) * (cell.jam_density_veh - cell.get_vehicles());
                gamma.put(lg.id, demand_to_me > supply ? supply / demand_to_me : 1d);
            }

            // lane change flow
            // WARNING: This assumes that no state has vehicles going in both directions.
            // ie a flow that goes left does not also go right. Otherwise I think there may
            // be "data races", where the result depends on the order of lgs.
            for (AbstractLaneGroup lg : link.lanegroups_flwdn.values()) {
                double my_gamma = gamma.get(lg.id);
                if (lg.neighbor_in != null) {
                    LaneGroup from_lg = (LaneGroup) lg.neighbor_in;
                    perform_lane_change(from_lg.cells.get(i).veh_out, (LaneGroup) lg, i, my_gamma);
                }
                if (lg.neighbor_out != null) {
                    LaneGroup from_lg = (LaneGroup) lg.neighbor_out;
                    perform_lane_change(from_lg.cells.get(i).veh_in, (LaneGroup) lg, i, my_gamma);
                }
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

    ////////////////////////////////////////////
    // private
    ///////////////////////////////////////////

    private static void perform_lane_change(Map<KeyCommPathOrLink, Double> from_vehs,LaneGroup to_lg,int i,double gamma){
        for (Map.Entry<KeyCommPathOrLink, Double> e : from_vehs.entrySet()) {

            Double from_veh = e.getValue();
            KeyCommPathOrLink state = e.getKey();

            if (from_veh > OTMUtils.epsilon) {

                double flw = gamma * from_veh;

                // remove from this cell
                from_vehs.put(state, from_veh-flw );

                // add to side cell
                Side newside = to_lg.state2lanechangedirection.containsKey(state) ?
                               to_lg.state2lanechangedirection.get(state) :
                               Side.full;
                Cell to_cell = to_lg.cells.get(i);
                switch (newside) {
                    case in:
                        to_cell.veh_in.put(state, to_cell.veh_in.get(state) + flw);
                        break;
                    case full:
                        to_cell.veh_dwn.put(state, to_cell.veh_dwn.get(state) + flw);
                        break;
                    case out:
                        to_cell.veh_out.put(state, to_cell.veh_out.get(state) + flw);
                        break;
                }
            }
        }

    }

}
