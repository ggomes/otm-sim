package models.fluid;

import core.FlowAccumulatorState;
import core.geometry.Side;
import core.State;

import java.util.Map;

public abstract class AbstractCell {

    public FluidLaneGroup laneGroup;

    public boolean am_upstrm;
    public boolean am_dnstrm;
    public double supply;   // [veh]
    public boolean in_barrier;
    public boolean out_barrier;

    public FlowAccumulatorState flw_acc;
    public FlowAccumulatorState flw_lcout_acc;
    public FlowAccumulatorState flw_lcin_acc;

    public abstract void reset();
    public abstract void allocate_state();
    public abstract Map<State,Double> get_demand();
    public abstract void update_supply();
    public abstract void update_demand();
    public abstract void add_vehicles(Map<State, Double> dwn, Map<State, Double> in, Map<State, Double> out);
    public abstract void add_vehicles(State state, Double value,Map<Side,Double> side2prob );
    public abstract void subtract_vehicles(Map<State, Double> dwn, Map<State, Double> in, Map<State, Double> out);
    public abstract double get_vehicles();
    public abstract double get_veh_dwn_for_commodity(Long comm_id);
    public abstract double get_veh_in_for_commodity(Long comm_id);
    public abstract double get_veh_out_for_commodity(Long comm_id);
    public abstract double get_veh_for_commodity(Long comm_id);

    public AbstractCell(FluidLaneGroup laneGroup) {
        this.am_upstrm = false;
        this.am_dnstrm = false;
        this.laneGroup = laneGroup;
    }

}
