package models.fluid;

import keys.KeyCommPathOrLink;

import java.util.Map;

public abstract class AbstractCell {

    public FluidLaneGroup laneGroup;

    public boolean am_upstrm;
    public boolean am_dnstrm;
    public double supply;   // [veh]
    public boolean in_barrier;
    public boolean out_barrier;

    public abstract void reset();
    public abstract void allocate_state();
    public abstract Map<KeyCommPathOrLink,Double> get_demand();
    public abstract void update_supply();
    public abstract void update_demand();
    public abstract void add_vehicles(Map<KeyCommPathOrLink, Double> dwn,Map<KeyCommPathOrLink, Double> in,Map<KeyCommPathOrLink, Double> out);
    public abstract void add_vehicles(KeyCommPathOrLink key,Double value);
    public abstract void subtract_vehicles(Map<KeyCommPathOrLink, Double> dwn,Map<KeyCommPathOrLink, Double> in,Map<KeyCommPathOrLink, Double> out);
    public abstract double get_vehicles();
    public abstract double get_veh_dwn_for_commodity(Long comm_id);
    public abstract double get_veh_in_for_commodity(Long comm_id);
    public abstract double get_veh_out_for_commodity(Long comm_id);
    public abstract double get_veh_for_commodity(Long comm_id);
    public abstract double get_total_vehicles();

    public AbstractCell(FluidLaneGroup laneGroup) {
        this.am_upstrm = false;
        this.am_dnstrm = false;
        this.laneGroup = laneGroup;
    }

}