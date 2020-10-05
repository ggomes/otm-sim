package models.fluid.ctm;

import geometry.Side;
import keys.State;
import models.fluid.AbstractCell;
import models.fluid.FluidLaneGroup;
import utils.OTMUtils;

import java.util.HashMap;
import java.util.Map;

public class CTMCell extends AbstractCell {

    // vehicles and demand already in their target lanegroup
    public Map<State, Double> veh_dwn;      // state -> number of vehicles
    public Map<State, Double> demand_dwn;   // state -> number of vehicles
    public double total_vehs_dwn;

    // vehicles wishing to change lanes outward (regardless if there is a barrier in this cell)
    public Map<State, Double> veh_out;      // state -> number of vehicles
    public Map<State, Double> demand_out;   // state -> number of vehicles
    public double total_vehs_out;

    // vehicles wishing to change lanes inward (regardless if there is a barrier in this cell)
    public Map<State, Double> veh_in;      // state -> number of vehicles
    public Map<State, Double> demand_in;   // state -> number of vehicles
    public double total_vehs_in;

    public CTMCell(FluidLaneGroup laneGroup) {
        super(laneGroup);
    }

    @Override
    public Map<State, Double> get_demand() {
        return demand_dwn;
    }

    @Override
    public void allocate_state() {

        // downstream flow
        veh_dwn = new HashMap<>();
        demand_dwn = new HashMap<>();
        for (State k : laneGroup.states) {
            veh_dwn.put(k, 0d);
            demand_dwn.put(k, 0d);
        }
        total_vehs_dwn = 0d;

        // outward flow
        if (laneGroup.neighbor_out != null) {
            veh_out = new HashMap<>();
            demand_out = new HashMap<>();
            for (State k : laneGroup.neighbor_out.states) {
                veh_out.put(k, 0d);
                demand_out.put(k, 0d);
            }
            total_vehs_out = 0d;
        }

        // inward flow
        if (laneGroup.neighbor_in != null) {
            veh_in = new HashMap<>();
            demand_in = new HashMap<>();
            for (State k : laneGroup.neighbor_in.states) {
                veh_in.put(k, 0d);
                demand_in.put(k, 0d);
            }
            total_vehs_in = 0d;
        }

    }

    @Override
    public void reset() {

    }

    @Override
    public void update_supply(){

        if(laneGroup.link.is_source)
            return;

        // update supply ..............................................
        if (laneGroup.link.is_sink)
            supply = laneGroup.capacity_veh_per_dt;
        else {


            // TODO REIMPLEMENT MN

//            switch (laneGroup.link.model_type) {
//                case ctm:
            double total_vehicles = get_vehicles();
            if(am_dnstrm)
                supply = Math.min(laneGroup.wspeed_cell_per_dt * (laneGroup.jam_density_veh_per_cell - total_vehicles), laneGroup.capacity_veh_per_dt);
            else {
                if(am_upstrm && laneGroup.link.is_model_source_link)
                    total_vehicles += laneGroup.buffer.get_total_veh();
                supply = laneGroup.wspeed_cell_per_dt * (laneGroup.jam_density_veh_per_cell - total_vehicles);
            }

        }
    }

    @Override
    public void update_demand(){

        double total_vehicles = total_vehs_dwn + total_vehs_out + total_vehs_in;

        double total_demand;

        // update demand ...................................................

        // case empty link
        if (total_vehicles < OTMUtils.epsilon) {
            demand_dwn.keySet().stream().forEach(k -> demand_dwn.put(k, 0d));
            if(demand_out!=null)
                demand_out.keySet().stream().forEach(k -> demand_out.put(k, 0d));
            if(demand_in!=null)
                demand_in.keySet().stream().forEach(k -> demand_in.put(k, 0d));
        }

        else {

            // compute total flow leaving the cell in the absence of flow control
            if (laneGroup.link.is_source)
                // sources discharge at capacity
                total_demand = Math.min(total_vehicles, laneGroup.capacity_veh_per_dt);
            else {
                // assume speed control acts equally on all cells in the link
                if(am_dnstrm)
                    total_demand = Math.min(laneGroup.ffspeed_cell_per_dt * total_vehicles, laneGroup.capacity_veh_per_dt);
                else
                    total_demand = laneGroup.ffspeed_cell_per_dt * total_vehicles;
            }

            // downstream cell: flow controller and lane change blocking
            if (am_dnstrm) {

                if(total_vehs_out+total_vehs_in>OTMUtils.epsilon) {
//                    double gamma = 0.9d;
//                    double mulitplier = Math.max(0d,1d-gamma*total_vehs_out);
//                    total_demand *= mulitplier;
                    total_demand = 0d;
                }
            }

            // split among states
            double alpha = total_demand / total_vehicles;
            for (State state : demand_dwn.keySet())
                demand_dwn.put(state, veh_dwn.get(state) * alpha);

            if(demand_out !=null)
                for (State key : demand_out.keySet())
                    demand_out.put(key, veh_out.get(key) * alpha);

            if(demand_in !=null)
                for (State key : demand_in.keySet())
                    demand_in.put(key, veh_in.get(key) * alpha);
        }
    }

    @Override
    public void add_vehicles(State state, Double vehs,Map<Side,Double> side2prob ){
        double cur_val;

        for(Map.Entry<Side,Double> e : side2prob.entrySet()){
            Side side = e.getKey();
            double val = e.getValue()*vehs;

            switch(side){
                case middle:
                    cur_val = veh_dwn.containsKey(state) ? veh_dwn.get(state) : 0d;
                    veh_dwn.put(state,cur_val + val);
                    total_vehs_dwn += val;
                    break;
                case in:
                    cur_val = veh_in.containsKey(state) ? veh_in.get(state) : 0d;
                    veh_in.put(state,cur_val + val);
                    total_vehs_in += val;
                    break;
                case out:
                    cur_val = veh_out.containsKey(state) ? veh_out.get(state) : 0d;
                    veh_out.put(state,cur_val + val);
                    total_vehs_out += val;
                    break;
            }

        }

    }

    @Override
    public void add_vehicles(Map<State, Double> dwn, Map<State, Double> in, Map<State, Double> out) {

        if (dwn != null) {
            for (Map.Entry<State, Double> e : dwn.entrySet()) {
                State state = e.getKey();
                double value = e.getValue();
                if (value > 0d) {
                    veh_dwn.put(state, veh_dwn.get(state) + value);
                    total_vehs_dwn += value;
                }
            }
        }

        if (in != null) {
            for (Map.Entry<State, Double> e : in.entrySet()) {
                State state = e.getKey();
                double value = e.getValue();
                if(value>0d) {
                    veh_in.put(state, veh_in.get(state) + value);
                    total_vehs_in += value;
                }
            }
        }

        if (out != null) {
            for (Map.Entry<State, Double> e : out.entrySet()) {
                State state = e.getKey();
                double value = e.getValue();
                if(value>0d) {
                    veh_out.put(state, veh_out.get(state) + value);
                    total_vehs_out += value;
                }
            }
        }
    }

    @Override
    public void subtract_vehicles(Map<State, Double> dwn, Map<State, Double> in, Map<State, Double> out) {

        if (dwn != null) {
            for (Map.Entry<State, Double> e : dwn.entrySet()) {
                State state = e.getKey();
                double value = e.getValue();
                if (value > 0d) {
                    veh_dwn.put(state, veh_dwn.get(state) - value);
                    total_vehs_dwn -= value;
                    if(flw_acc!=null)
                        flw_acc.increment(state,value);
                }
            }
        }

        if (in != null) {
            for (Map.Entry<State, Double> e : in.entrySet()) {
                State state = e.getKey();
                double value = e.getValue();
                if(value>0d) {
                    veh_in.put(state, veh_in.get(state) - value);
                    total_vehs_in -= value;
                    if(flw_acc!=null)
                        flw_acc.increment(state,value);
                }
            }
        }

        if (out != null) {
            for (Map.Entry<State, Double> e : out.entrySet()) {
                State state = e.getKey();
                double value = e.getValue();
                if(value>0d) {
                    veh_out.put(state, veh_out.get(state) - value);
                    total_vehs_out -= value;
                    if(flw_acc!=null)
                        flw_acc.increment(state,value);
                }
            }
        }
    }

    @Override
    public double get_veh_dwn_for_commodity(Long comm_id) {
        if(comm_id==null)
            return total_vehs_dwn;
        else
            return veh_dwn.entrySet().stream()
                    .filter(x->x.getKey().commodity_id==comm_id)
                    .mapToDouble(x->x.getValue())
                    .sum();
    }

    @Override
    public double get_veh_in_for_commodity(Long comm_id) {
        if(comm_id==null)
            return this.total_vehs_in;
        else {
            return veh_in==null ? 0d : veh_in.entrySet().stream()
                    .filter(x -> x.getKey().commodity_id == comm_id)
                    .mapToDouble(x -> x.getValue())
                    .sum();
        }
    }

    @Override
    public double get_veh_out_for_commodity(Long comm_id) {
        if(comm_id==null)
            return this.total_vehs_out;
        else
            return veh_out==null ? 0d : veh_out.entrySet().stream()
                    .filter(x->x.getKey().commodity_id==comm_id)
                    .mapToDouble(x->x.getValue())
                    .sum();
    }

    @Override
    public double get_veh_for_commodity(Long comm_id) {
        return get_veh_dwn_for_commodity(comm_id) + get_veh_in_for_commodity(comm_id) + get_veh_out_for_commodity(comm_id);
    }

    @Override
    public double get_vehicles() {
        return total_vehs_dwn + total_vehs_in + total_vehs_out;
    }

}
