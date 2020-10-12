package models.fluid;

import common.*;
import error.OTMErrorLog;
import error.OTMException;
import geometry.FlowPosition;
import geometry.Side;
import jaxb.Roadparam;
import keys.State;
import packet.PacketLaneGroup;
import utils.OTMUtils;

import java.util.*;

public class FluidLaneGroup extends AbstractLaneGroup {

    // nominal fd
    public double nom_ffspeed_cell_per_dt;         // [-]
    public double nom_capacity_veh_per_dt;

    // actual (actuated) parameters
    public double wspeed_cell_per_dt;          // [-]

    public double lc_w;          // = 0.9d * (1d - lg.wspeed_cell_per_dt) / lg.wspeed_cell_per_dt;
    public double ffspeed_cell_per_dt;         // [-]
    public double capacity_veh_per_dt;
    public double jam_density_veh_per_cell;
    public double critical_density_veh;

    public List<AbstractCell> cells;     // sequence of cells

    ////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public FluidLaneGroup(Link link, Side side, FlowPosition flwpos, float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs, jaxb.Roadparam rp) {
        super(link, side, flwpos,length, num_lanes, start_lane, out_rcs,rp);
    }

    ////////////////////////////////////////////
    // InterfaceScenarioElement-like
    ///////////////////////////////////////////

    @Override
    public void validate(OTMErrorLog errorLog) {

        if (jam_density_veh_per_cell < 0)
            errorLog.addError("non-negativity");

        if (!link.is_source) {
            if (ffspeed_cell_per_dt < 0)
                errorLog.addError("ffspeed_cell_per_dt < 0 (link " + link.getId() + ")");
            if (wspeed_cell_per_dt < 0)
                errorLog.addError("wspeed_cell_per_dt < 0 (link " + link.getId() + ")");
            if (wspeed_cell_per_dt > 1)
                errorLog.addError("CFL violated: link " + link.getId() + " wspeed_cell_per_dt = " + wspeed_cell_per_dt);
            if (ffspeed_cell_per_dt > 1)
                errorLog.addError("CFL violated: link " + link.getId() + " ffspeed_cell_per_dt = " + ffspeed_cell_per_dt);
        }
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);

        if(!cells.isEmpty() && cells.get(0).flw_acc!=null)
            cells.forEach(c->c.flw_acc.reset());
        if(!cells.isEmpty() && cells.get(0).flw_lcout_acc!=null)
            cells.forEach(c->c.flw_lcout_acc.reset());
        if(!cells.isEmpty() && cells.get(0).flw_lcin_acc!=null)
            cells.forEach(c->c.flw_lcin_acc.reset());
    }

    ////////////////////////////////////////////
    // InterfaceLaneGroup
    ///////////////////////////////////////////

    @Override
    public void set_road_params(Roadparam r) {

        // adjustment for MN model
        // TODO REIMPLIMENT MN
//        if(link.model_type==Link.ModelType.mn)
//            r.setJamDensity(Float.POSITIVE_INFINITY);

        float dt_sec = ((AbstractFluidModel)link.model).dt_sec;
        if(Float.isNaN(dt_sec))
            return;

        // normalize
        float dt_hr = dt_sec /3600f;
        float capacity_vehperlane = r.getCapacity()*dt_hr;
        float jam_density_vehperkmperlane = r.getJamDensity();
        float ffspeed_veh = r.getSpeed() * dt_hr;   // [km/dt]

        nom_capacity_veh_per_dt = capacity_vehperlane * num_lanes;
        if (link.is_source) {
            nom_ffspeed_cell_per_dt = Double.NaN;
            ffspeed_cell_per_dt = Double.NaN;
            jam_density_veh_per_cell = Double.NaN;
            critical_density_veh = Double.NaN;
            wspeed_cell_per_dt = Double.NaN;
            lc_w = Double.NaN;
            capacity_veh_per_dt = nom_capacity_veh_per_dt;
        } else {
            nom_ffspeed_cell_per_dt = ffspeed_veh;                                  // /cell_length in build
            ffspeed_cell_per_dt = ffspeed_veh;                                      // /cell_length in build
            jam_density_veh_per_cell = jam_density_vehperkmperlane * num_lanes;     // *cell_length in build
            double critical_vehperlane = capacity_vehperlane / nom_ffspeed_cell_per_dt;
            critical_density_veh = critical_vehperlane * num_lanes;          // *lg_length in build
            wspeed_cell_per_dt = capacity_vehperlane / (jam_density_vehperkmperlane - critical_vehperlane);// /cell_length in build
            compute_lcw();
            capacity_veh_per_dt = nom_capacity_veh_per_dt;
        }
    }

    @Override
    public void set_actuator_capacity_vps(double rate_vps) {
        double act_capacity_veh_per_dt = rate_vps * ((AbstractFluidModel)link.model).dt_sec;
        this.capacity_veh_per_dt = Math.min(act_capacity_veh_per_dt,nom_capacity_veh_per_dt);

        // set w
//        double critical_veh = capacity_veh_per_dt / ffspeed_cell_per_dt;
//        wspeed_cell_per_dt = capacity_veh_per_dt / (jam_density_veh_per_cell -critical_veh);
    }

    @Override
    public void set_actuator_speed_mps(double speed_mps) {
        float cell_length = this.length / this.cells.size();
        float dt_sec = ((AbstractFluidModel)link.model).dt_sec;
        float act_ffspeed_veh = ((float)speed_mps) * dt_sec / cell_length;
        this.ffspeed_cell_per_dt = Math.min(act_ffspeed_veh,nom_ffspeed_cell_per_dt);

        // set w
        double critical_vehperlane = capacity_veh_per_dt / ffspeed_cell_per_dt;
        critical_density_veh = critical_vehperlane * num_lanes;
        wspeed_cell_per_dt = capacity_veh_per_dt / (jam_density_veh_per_cell -critical_vehperlane);
        compute_lcw();
    }

    @Override
    public void allocate_state() {
        cells.forEach(c -> c.allocate_state());
    }

    @Override
    public double get_max_vehicles() {
        return jam_density_veh_per_cell *cells.size();
    }

    @Override
    public Double get_upstream_vehicle_position(){
        return Double.NaN;
    }

    @Override
    public void add_vehicle_packet(float timestamp, PacketLaneGroup vp, Long nextlink_id) {

        AbstractCell cell = cells.get(0);

        // When the link is a model source, then the packet first goes into a buffer.
        // From there it is "processed", meaning that some part goes into the upstream cell.
        if(link.is_model_source_link) {
            // add packet to buffer
            assert(false); // DOES THIS EVER HAPPEN? PERHAPS SOURCE FLOWS ARE PROC ESSED IN UPDATE_FLOW_II AND
                            // VEHICLES ARE PLACED DIRECTLY INTO THE UPSTREAM CELL
            buffer.add_packet(vp);
            process_buffer(timestamp);
        }

        // otherwise, this is an internal link, and the packet is guaranteed to be
        // purely fluid.
        else {
            for(Map.Entry<State,Double> e : vp.container.amount.entrySet()) {
                State state = e.getKey();

                // update state
                if(!state.isPath)
                    state = new State(state.commodity_id,nextlink_id,false);

                Set<Side> lcoptions = state2lanechangedirections.get(state);

                Map<Side,Double> side2prob = get_lc_probabilities(state,lcoptions);

                cell.add_vehicles(state,e.getValue(),side2prob);
            }
        }
        update_supply();
    }

    @Override
    public void release_vehicle_packets(float timestamp) throws OTMException {
        throw new OTMException("This should not be called.");
    }

    @Override
    public void update_supply(){

        // shut off if buffer is full
        if(link.is_model_source_link && buffer.get_total_veh()>=1d)
            supply = 0d;
        else {
            AbstractCell upcell = get_upstream_cell();
            upcell.update_supply();
            supply = upcell.supply;
        }

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

    ////////////////////////////////////////////
    // lane change model
    ////////////////////////////////////////////

    // This is called when vehicles are added to the first cell in a lanegroup.
    // They decide which way to chenge lanes within the lanegroup.
    public Map<Side,Double> get_lc_probabilities(State state,Set<Side> lcoptions){

        // otherwise use the lane selector, if it exists
        if(lane_selector!=null && lane_selector.containsKey(state.commodity_id))
            return lane_selector.get(state.commodity_id).get_lanechange_probabilities(state.pathOrlink_id);

        // otherwise distribute equally
        double v = 1d/lcoptions.size();
        Map<Side,Double> X = new HashMap<>();
        for(Side s:lcoptions)
            X.put(s,v);
        return X;

    }

    ////////////////////////////////////////////
    // helper methods (final)
    ////////////////////////////////////////////

    public final int get_num_cells(){
        return cells.size();
    }

    public final List<FlowAccumulatorState> request_flow_accumulators_for_cells(Long comm_id){
        List<FlowAccumulatorState> X = new ArrayList<>();
        for(AbstractCell cell : cells){
            if(cell.flw_acc==null)
                cell.flw_acc = new FlowAccumulatorState();
            for(State state : states)
                if(comm_id==null || state.commodity_id==comm_id)
                    cell.flw_acc.add_state(state);
            X.add(cell.flw_acc);
        }
        return X;
    }

    public final FlowAccumulatorState request_flow_accumulators_for_cell(Set<Long> comm_ids,int cell_index){
        AbstractCell cell = cells.get(cell_index);
        if(cell.flw_acc==null)
            cell.flw_acc = new FlowAccumulatorState();
        for(State state : states)
            if(comm_ids==null || comm_ids.contains(state.commodity_id))
                cell.flw_acc.add_state(state);
        return cell.flw_acc;
    }

    public final List<FlowAccumulatorState> request_flow_lcout_accumulators_for_cells(Long comm_id){
        List<FlowAccumulatorState> X = new ArrayList<>();
        for(AbstractCell cell : cells){
            if(cell.flw_lcout_acc==null)
                cell.flw_lcout_acc = new FlowAccumulatorState();
            for(State state : states)
                if(comm_id==null || state.commodity_id==comm_id)
                    cell.flw_lcout_acc.add_state(state);
            X.add(cell.flw_lcout_acc);
        }
        return X;
    }

    public final List<FlowAccumulatorState> request_flow_lcin_accumulators_for_cells(Long comm_id){
        List<FlowAccumulatorState> X = new ArrayList<>();
        for(AbstractCell cell : cells){
            if(cell.flw_lcin_acc==null)
                cell.flw_lcin_acc = new FlowAccumulatorState();
            for(State state : states)
                if(comm_id==null || state.commodity_id==comm_id)
                    cell.flw_lcin_acc.add_state(state);
            X.add(cell.flw_lcin_acc);
        }
        return X;
    }

    public final AbstractCell get_upstream_cell(){
        return cells.get(0);
    }

    public final AbstractCell get_dnstream_cell(){
        return cells.get(cells.size()-1);
    }

    public final Map<State,Double> get_demand(){
        return get_dnstream_cell().get_demand();
    }

    public final void create_cells(AbstractFluidModel model,float cell_length_meters) throws OTMException {

        int num_cells = Math.round(this.length/cell_length_meters);

        // create the cells
        this.cells = new ArrayList<>();
        for(int i=0;i<num_cells;i++)
            this.cells.add(model.create_cell(this));

        // designate first and last
        this.cells.get(0).am_upstrm = true;
        this.cells.get(num_cells-1).am_dnstrm = true;
    }

    //////////////////////////////////////////
    // SHOULD THESE BE MOVED TO CTM?
    ///////////////////////////////////////////

    public void release_vehicles(Map<State,Double> X){
        cells.get(cells.size()-1).subtract_vehicles(X,null,null);

        // if this is a single cell lane group, then releasing a vehicle will affect the supply
        if(cells.size()==1)
            update_supply();
    }

    public void process_buffer(float timestamp){
        assert(link.is_model_source_link);

        double buffer_size = buffer.get_total_veh();

        if(buffer_size < OTMUtils.epsilon )
            return;

        AbstractCell cell = cells.get(0);
                    double total_space = cell.supply;
//        double total_space = jam_density_veh_per_cell - cell.get_vehicles();
        double factor = Math.min( 1d , total_space / buffer_size );
        for(Map.Entry<State,Double> e : buffer.amount.entrySet()) {
            State state = e.getKey();
            Double buffer_vehs = e.getValue() ;

            Set<Side> lcoptions = state2lanechangedirections.get(state);

            Map<Side,Double> side2prob = get_lc_probabilities(state,lcoptions);

            // add to cell
            cell.add_vehicles(state,buffer_vehs* factor,side2prob);

            // remove from buffer
            e.setValue(buffer_vehs*(1d-factor));
        }

    }

    public void compute_lcw(){
        lc_w = .9d * (1d - wspeed_cell_per_dt) / wspeed_cell_per_dt;
    }

}














