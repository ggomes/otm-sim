/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.ctm;

import common.AbstractLaneGroupLongitudinal;
import error.OTMErrorLog;
import keys.KeyCommPathOrLink;
import utils.OTMUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Cell {

    public LinkModel model;
    private LaneGroupLong laneGroup;

    public Cell neighbor;

    public boolean am_upstrm;
    public boolean am_dnstrm;

    // road params
    public double capacity_veh;         // [veh]
    public double wspeed_norm;          // [-]
    public double ffspeed_norm;         // [-]
    public double jam_density_veh;      // [veh]

    // vehicles and demand already in their target lanegroup
    public Map<KeyCommPathOrLink, Double> veh_in_target;      // comm,path|nlink -> number of vehicles
    public Map<KeyCommPathOrLink, Double> demand_in_target;   // comm,path|nlink -> number of vehicles

    // vehicles and demand not in their target lanegroup
    public Map<KeyCommPathOrLink, Double> veh_notin_target;      // comm,path|nlink -> number of vehicles
    public Map<KeyCommPathOrLink, Double> demand_notin_target;   // comm,path|nlink -> number of vehicles

    // lane change flow
    public Map<KeyCommPathOrLink, Double> lane_change_flow;      // comm,path|nlink -> number of vehicles

    public double supply;   // [veh]

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public Cell(LinkModel model, double length_in_meters, LaneGroupLong laneGroup) {
        this.am_upstrm = false;
        this.am_dnstrm = false;
        this.model = model;
        this.laneGroup = laneGroup;
    }

    public void set_road_params(float capacity_vehperlane, float jam_density_vehperlane, float ffspeed_veh) {
        int lanes = laneGroup.num_lanes;
        if (model.link.is_source) {
            this.capacity_veh = capacity_vehperlane * lanes;
            this.ffspeed_norm = Double.NaN;
            this.jam_density_veh = Double.NaN;
            this.wspeed_norm = Double.NaN;
        } else {
            this.capacity_veh = capacity_vehperlane * lanes;
            this.ffspeed_norm = ffspeed_veh;
            this.jam_density_veh = jam_density_vehperlane * lanes;
            double critical_veh = capacity_veh / ffspeed_norm;
            this.wspeed_norm = capacity_veh / (jam_density_veh - critical_veh);
        }
    }

    public void validate(OTMErrorLog errorLog) {

        if (!model.link.is_source) {
            if (ffspeed_norm < 0)
                errorLog.addError("non-negativity");
            if (jam_density_veh < 0)
                errorLog.addError("non-negativity");
            if (wspeed_norm < 0)
                errorLog.addError("non-negativity");
            if (wspeed_norm > 1)
                errorLog.addError("CFL violated: link " + laneGroup.link.getId() + " wspeed_norm = " + wspeed_norm);
            if (ffspeed_norm > 1)
                errorLog.addError("CFL violated: link " + laneGroup.link.getId() + " ffspeed_norm = " + ffspeed_norm);
        }

        // NOTE: THIS IS FAILING BECAUSE LANES HAVE NOT BEEN SET
//        if(capacity_veh<=0)
//            scenario.error_log.addError("capacity must be positive");
    }

    public void reset(){

    }

    public void allocate_state() {

        // this == target lane group
        veh_in_target = new HashMap<>();
        demand_in_target = new HashMap<>();
        for (KeyCommPathOrLink k : laneGroup.states) {
            veh_in_target.put(k, 0d);
            demand_in_target.put(k, 0d);
        }

        // this != target lane group
        if (laneGroup.neighbor_out != null) { // TODO FIX THIS
            veh_notin_target = new HashMap<>();
            demand_notin_target = new HashMap<>();
            lane_change_flow = new HashMap<>();
            for (KeyCommPathOrLink k : laneGroup.neighbor_out.states) {
                veh_notin_target.put(k, 0d);
                demand_notin_target.put(k, 0d);
                lane_change_flow.put(k, 0d);
            }
        }

    }

    ///////////////////////////////////////////////////
    // get
    ///////////////////////////////////////////////////

    public double get_vehicles_in_target() {
        return OTMUtils.sum(veh_in_target.values());
    }

    public double get_vehicles_notin_target() {
        return veh_notin_target == null ? 0d : OTMUtils.sum(veh_notin_target.values());
    }

    public double get_vehicles() {
        return get_vehicles_in_target() + get_vehicles_notin_target();
    }

    public double get_vehicles_for_commodity(Long commodity_id) {

        if (commodity_id == null)
            return get_vehicles();

        double veh = 0d;
        for (Map.Entry<KeyCommPathOrLink, Double> e : this.veh_in_target.entrySet())
            if (e.getKey().commodity_id == commodity_id)
                veh += e.getValue();
        return veh;
    }

    ///////////////////////////////////////////////////
    // update
    ///////////////////////////////////////////////////

    public void update_lane_change_flow() {

        if(neighbor==null)
            return;

        double total_veh_notin_target = get_vehicles_notin_target();

        if(total_veh_notin_target<=OTMUtils.epsilon) {
            lane_change_flow.keySet().forEach(x->lane_change_flow.put(x,0d));
            return;
        }

        double total_neighbor_veh = neighbor.get_vehicles();

        double xi = 0.5*(1-neighbor.wspeed_norm);   // TODO FIX THIS!!!

        // total lane changing flow
        double total_flow = Math.min(total_veh_notin_target, xi * (neighbor.jam_density_veh - total_neighbor_veh));

        // save lane change flows per state
        for (Map.Entry<KeyCommPathOrLink, Double> e : veh_notin_target.entrySet()) {
            Double veh = e.getValue();
            if (veh > 0)
                lane_change_flow.put(e.getKey(), total_flow * veh / total_veh_notin_target);
        }

    }

    public void intermediate_state_update(){

        if(neighbor==null || lane_change_flow==null)
            return;

        // vehicles leaving this cell
        for (Map.Entry<KeyCommPathOrLink, Double> e : lane_change_flow.entrySet()) {
            Double veh = e.getValue();
            if (veh > 0) {
                KeyCommPathOrLink state = e.getKey();
                veh_notin_target.put(state,veh_notin_target.get(state)-veh);
                neighbor.veh_in_target.put(state,neighbor.veh_in_target.get(state) + veh);
            }
        }

    }

    // compute demand_in_target (demand per commodity and path)
    // and supply (total supply)
    public void update_supply_demand() {

        double vehicles_in_target = get_vehicles_in_target();
        double vehicles_notin_target = get_vehicles_notin_target();
        double total_vehicles = vehicles_in_target + vehicles_notin_target;

        double external_max_speed = Double.POSITIVE_INFINITY;
        double total_demand;

        // update demand ...................................................

        // case empty link
        if (total_vehicles < OTMUtils.epsilon) {
            demand_in_target.keySet().stream().forEach(k -> demand_in_target.put(k, 0d));
            if(demand_notin_target!=null)
                demand_notin_target.keySet().stream().forEach(k -> demand_notin_target.put(k, 0d));
        }

        else {

            // compute total flow leaving the cell in the absence of flow control
            if (model.link.is_source)
                // sources discharge at capacity
                total_demand = Math.min(total_vehicles, capacity_veh);
            else {
                // assume speed control acts equally on all cells in the link
                double ffspeed = Math.min(ffspeed_norm, external_max_speed);
                if(am_dnstrm)
                    total_demand = Math.min(ffspeed * total_vehicles, capacity_veh);
                else
                    total_demand = ffspeed * total_vehicles;
            }

            // downstream cell: flow controller and lane change blocking
            if (am_dnstrm) {

                if(vehicles_notin_target>OTMUtils.epsilon) {
                    double gamma = 0.9d;
                    double mulitplier = Math.max(0d,1d-gamma*vehicles_notin_target);
                    total_demand *= mulitplier;
                }
            }

            // split among in|out target, commodities, paths|nextlinks
            double alpha = total_demand / total_vehicles;
            for (KeyCommPathOrLink key : demand_in_target.keySet())
                demand_in_target.put(key, veh_in_target.get(key) * alpha);

            if(demand_notin_target!=null)
                for (KeyCommPathOrLink key : demand_notin_target.keySet())
                    demand_notin_target.put(key, veh_notin_target.get(key) * alpha);
        }

        // update supply ..............................................
        if (model.link.is_sink)
            supply = capacity_veh;
        else {
            switch (model.link.model_type) {
                case ctm:
                    if(am_dnstrm)
                        supply = Math.min(wspeed_norm * (jam_density_veh - total_vehicles), capacity_veh);
                    else
                        supply = wspeed_norm * (jam_density_veh - total_vehicles);
                    break;
                case mn:
                    supply = Float.POSITIVE_INFINITY;
                    break;
                default:
                    System.err.println("Wha??");
            }
        }
    }

    public void update_in_target_state(Map<KeyCommPathOrLink, Double> inflow, Map<KeyCommPathOrLink, Double> outflow) {

        if (inflow != null)
            for (Map.Entry<KeyCommPathOrLink, Double> e : inflow.entrySet()) {
                KeyCommPathOrLink state = e.getKey();
                veh_in_target.put(state, veh_in_target.get(state) + e.getValue());
            }

        if (outflow != null)
            for (Map.Entry<KeyCommPathOrLink, Double> e : outflow.entrySet()) {
                KeyCommPathOrLink state = e.getKey();
                veh_in_target.put(state, veh_in_target.get(state) - e.getValue());
            }

    }

    public void update_notin_target_state(Map<KeyCommPathOrLink, Double> inflow, Map<KeyCommPathOrLink, Double> outflow) {

        if (inflow != null)
            for (Map.Entry<KeyCommPathOrLink, Double> e : inflow.entrySet()) {
                KeyCommPathOrLink state = e.getKey();
                veh_notin_target.put(state, veh_notin_target.get(state) + e.getValue());
            }

        if (outflow != null)
            for (Map.Entry<KeyCommPathOrLink, Double> e : outflow.entrySet()) {
                KeyCommPathOrLink state = e.getKey();
                veh_notin_target.put(state, veh_notin_target.get(state) - e.getValue());
            }
    }
}