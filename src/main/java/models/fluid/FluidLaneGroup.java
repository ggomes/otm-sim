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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FluidLaneGroup extends AbstractLaneGroup {

    public float cell_length_meters;

    // nominal fd
    public double nom_ffspeed_cell_per_dt;         // [-]
    public double nom_capacity_veh_per_dt;

    // actual (actuated) parameters
    public double wspeed_cell_per_dt;          // [-]
    public double ffspeed_cell_per_dt;         // [-]
    public double capacity_veh_per_dt;
    public double jam_density_veh_per_cell;

    public List<AbstractCell> cells;     // sequence of cells

    ////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public FluidLaneGroup(Link link, Side side, FlowPosition flwpos, float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs) {
        super(link, side, flwpos,length, num_lanes, start_lane, out_rcs);
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

        float cell_length = length / cells.size() / 1000f;
        float jam_density_vehperlane = r.getJamDensity() * cell_length;
        float ffspeed_veh = r.getSpeed() * dt_hr / cell_length;

        nom_capacity_veh_per_dt = capacity_vehperlane * num_lanes;
        if (link.is_source) {
            nom_ffspeed_cell_per_dt = Double.NaN;
            jam_density_veh_per_cell = Double.NaN;
            wspeed_cell_per_dt = Double.NaN;
        } else {
            nom_ffspeed_cell_per_dt = ffspeed_veh;
            jam_density_veh_per_cell = jam_density_vehperlane * num_lanes;
            double critical_veh = capacity_vehperlane / ffspeed_veh;
            wspeed_cell_per_dt = capacity_vehperlane / (jam_density_vehperlane - critical_veh);
        }

        capacity_veh_per_dt = nom_capacity_veh_per_dt;
        ffspeed_cell_per_dt = nom_ffspeed_cell_per_dt;
    }

    @Override
    public void set_actuator_capacity_vps(double rate_vps) {
        double act_capacity_veh_per_dt = rate_vps * ((AbstractFluidModel)link.model).dt_sec;
        this.capacity_veh_per_dt = Math.min(act_capacity_veh_per_dt,nom_capacity_veh_per_dt);

        // set w
        double critical_veh = capacity_veh_per_dt / ffspeed_cell_per_dt;
        wspeed_cell_per_dt = capacity_veh_per_dt / (jam_density_veh_per_cell -critical_veh);
    }

    @Override
    public void set_actuator_speed_mps(double speed_mps) {
        float cell_length = this.length / this.cells.size();
        float dt_sec = ((AbstractFluidModel)link.model).dt_sec;
        float act_ffspeed_veh = ((float)speed_mps) * dt_sec / cell_length;
        this.ffspeed_cell_per_dt = Math.min(act_ffspeed_veh,nom_ffspeed_cell_per_dt);

        // set w
        double critical_veh = capacity_veh_per_dt / ffspeed_cell_per_dt;
        wspeed_cell_per_dt = capacity_veh_per_dt / (jam_density_veh_per_cell -critical_veh);
    }

    @Override
    public void allocate_state() {
        cells.forEach(c -> c.allocate_state());
    }

    @Override
    public double get_max_vehicles() {
        return jam_density_veh_per_cell*cells.size();
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

                cell.add_vehicles(state,e.getValue());
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
    // helper methods (final)
    ////////////////////////////////////////////

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

    public final AbstractCell get_upstream_cell(){
        return cells.get(0);
    }

    public final AbstractCell get_dnstream_cell(){
        return cells.get(cells.size()-1);
    }

    public final Map<State,Double> get_demand(){
        return get_dnstream_cell().get_demand();
    }





    // THIS CAN PROBABLY BE REMOVED .........................

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
//
//    @Override
//    public float get_current_travel_time() {
//        double sim_dt = ((models.fluid.ctm.Model_CTM)link.model).dt;
//        float sum = 0f;
//        for(int i=0;i<cells.size();i++){
//            Cell cell = cells.get(i);
//
//            double tt;
//            double veh = cell.get_vehicles();   // [veh]
//
//            if(veh>0) {
//
//                Map<KeyCommPathOrLink,Double> bf = flow_stay.get(i+1);
//                double out_flow = bf==null ? 0d : bf.values().stream().mapToDouble(x->x).sum();
//
//                if(out_flow==0)
//                    tt = link.is_source ? sim_dt : sim_dt / ffspeed_cell_per_dt;
//                else
//                    tt = sim_dt * veh / out_flow;
//
//            } else
//                tt = link.is_source ? sim_dt : sim_dt / ffspeed_cell_per_dt;
//
//            sum += tt;
//        }
//        return sum;
//    }
    // .............................................................

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
            State key = e.getKey();
            Double buffer_vehs = e.getValue() ;

            // add to cell
            cell.add_vehicles(key,buffer_vehs* factor);

            // remove from buffer
            e.setValue(buffer_vehs*(1d-factor));
        }

    }
}














