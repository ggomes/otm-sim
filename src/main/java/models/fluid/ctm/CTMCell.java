package models.fluid.ctm;

import core.State;
import models.Maneuver;
import models.fluid.AbstractCell;
import models.fluid.FluidLaneGroup;
import utils.OTMUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CTMCell extends AbstractCell {

    // vehicles and demand already in their target lanegroup
    protected Map<State, Double> veh_dwn;      // state -> number of vehicles
    protected Map<State, Double> demand_dwn;   // state -> number of vehicles
    protected double total_vehs_dwn;

    // vehicles wishing to change lanes outward (regardless if there is a barrier in this cell)
    protected Map<State, Double> veh_out;      // state -> number of vehicles
    protected Map<State, Double> demand_out;   // state -> number of vehicles
    protected double total_vehs_out;

    // vehicles wishing to change lanes inward (regardless if there is a barrier in this cell)
    protected Map<State, Double> veh_in;      // state -> number of vehicles
    protected Map<State, Double> demand_in;   // state -> number of vehicles
    protected double total_vehs_in;

    public CTMCell(FluidLaneGroup laneGroup) {
        super(laneGroup);
    }

    @Override
    public Map<State, Double> get_demand() {
        return demand_dwn;
    }

    @Override
    public void allocate_state() {

        Set<State> states = laneGroup.get_link().states;

        // downstream flow
        veh_dwn = new HashMap<>();
        demand_dwn = new HashMap<>();
        for (State k : states) {
            veh_dwn.put(k, 0d);
            demand_dwn.put(k, 0d);
        }
        total_vehs_dwn = 0d;

        // outward flow
        if (laneGroup.get_neighbor_out() != null) {
            veh_out = new HashMap<>();
            demand_out = new HashMap<>();
            for (State k : states) {
                veh_out.put(k, 0d);
                demand_out.put(k, 0d);
            }
            total_vehs_out = 0d;
        }

        // inward flow
        if (laneGroup.get_neighbor_in() != null) {
            veh_in = new HashMap<>();
            demand_in = new HashMap<>();
            for (State k : states) {
                veh_in.put(k, 0d);
                demand_in.put(k, 0d);
            }
            total_vehs_in = 0d;
        }

    }

    @Override
    public void set_state() {

    }

    @Override
    public void update_supply(){

        if(laneGroup.get_link().is_source())
            return;

        // update supply ..............................................
        if (laneGroup.get_link().is_sink())
            supply = laneGroup.capacity_veh_per_dt;
        else {


            // TODO REIMPLEMENT MN

//            switch (laneGroup.link.model_type) {
//                case ctm:
            double total_vehicles = get_vehicles();
            if(am_dnstrm)
                supply = Math.min(laneGroup.wspeed_cell_per_dt * (laneGroup.jam_density_veh_per_cell - total_vehicles), laneGroup.capacity_veh_per_dt);
            else {
                if(am_upstrm && laneGroup.get_link().is_model_source_link())
                    total_vehicles += laneGroup.buffer.get_total_veh();
                supply = laneGroup.wspeed_cell_per_dt * (laneGroup.jam_density_veh_per_cell - total_vehicles);
            }

        }
    }

    @Override
    public void update_demand(){

        double total_vehicles = total_vehs_dwn + total_vehs_out + total_vehs_in;

        // case empty link
        if (total_vehicles < OTMUtils.epsilon) {
            demand_dwn.keySet().stream().forEach(k -> demand_dwn.put(k, 0d));
            if(demand_out!=null)
                demand_out.keySet().stream().forEach(k -> demand_out.put(k, 0d));
            if(demand_in!=null)
                demand_in.keySet().stream().forEach(k -> demand_in.put(k, 0d));
            return;
        }

        // compute total demand
        double total_demand;
        boolean block = ((ModelCTM) laneGroup.get_link().get_model()).block;
        if (laneGroup.get_link().is_source()) {
            // sources discharge at capacity
            total_demand = Math.min(total_vehicles, laneGroup.capacity_veh_per_dt);
            if(laneGroup.get_link().getId()==2l && laneGroup.get_start_lane_dn()==1){
                System.out.println(total_vehicles*720f);
            }
        }
        else {
            if(am_dnstrm)
                if ( block && total_vehs_out + total_vehs_in > OTMUtils.epsilon)
                    total_demand = 0d;
                else
                    total_demand = Math.min(laneGroup.ffspeed_cell_per_dt * total_vehicles, laneGroup.capacity_veh_per_dt);
            else
                total_demand = laneGroup.ffspeed_cell_per_dt * total_vehicles;
        }

        // no blocking strategy: lane change demand is converted to dwn demand to an alternative link
        // For simplicity, we only provide a single alternative for all states and lane change directions.
        // The general solution would be to provide a map from (state,out or in) -> new state.
        // Here we have ( *, * ) -> new linkid (this does not work for path-full commodities).
        // This is specified as default_next_link in the link schema.
        if (am_dnstrm && !block) {

            Long alt_next_link = laneGroup.get_link().alt_next_link;

            if(alt_next_link!=null) {
                double vcl, vclout, vclin;
                for (State state : laneGroup.get_link().states) {
                    // this cannot be done for pathfull commodities
                    if (state.isPath)
                        continue;
                    if (state.pathOrlink_id == alt_next_link)
                        continue;

                    vcl = 0d;
                    if (veh_out != null) {
                        vclout = veh_out.get(state);
                        vcl += vclout;
                        veh_out.put(state, 0d);
                        total_vehs_out -= vclout;
                    }
                    if (veh_in != null) {
                        vclin = veh_in.get(state);
                        vcl += vclin;
                        veh_in.put(state, 0d);
                        total_vehs_in -= vclin;
                    }

                    if (vcl > OTMUtils.epsilon) {
                        State newstate = new State(state.commodity_id, alt_next_link, false);
                        veh_dwn.put(newstate, veh_dwn.get(newstate) + veh_dwn.get(state) + vcl);
                        total_vehs_dwn += vcl;
                    }

                }
            }

            double alpha = total_demand / total_vehicles;
            for (State state : laneGroup.get_link().states)
                demand_dwn.put(state, veh_dwn.get(state) * alpha);

            return;
        }

        // standard strategy
        double alpha = total_demand / total_vehicles;
        for (State state : laneGroup.get_link().states) {
            demand_dwn.put(state, veh_dwn.get(state) * alpha);
            if(veh_out !=null)
                demand_out.put(state, veh_out.get(state) * alpha);
            if(veh_in !=null)
                demand_in.put(state, veh_in.get(state) * alpha);
        }

    }

    @Override
    public void add_vehicles(State state, Double vehs,Map<Maneuver,Double> maneuver2prob ){
        double cur_val;

        if(vehs<=0)
            return;

        for(Map.Entry<Maneuver,Double> e : maneuver2prob.entrySet()){
            Maneuver side = e.getKey();

            if(e.getValue()<=0)
                continue;

            double val = e.getValue()*vehs;

            switch(side){

                case stay:
                    cur_val = veh_dwn.containsKey(state) ? veh_dwn.get(state) : 0d;
                    veh_dwn.put(state,cur_val + val);
                    total_vehs_dwn += val;
                    break;

                case lcin:
                    cur_val = veh_in.containsKey(state) ? veh_in.get(state) : 0d;
                    veh_in.put(state,cur_val + val);
                    total_vehs_in += val;
                    break;

                case lcout:
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
