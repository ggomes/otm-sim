package models.fluid;

import common.Link;
import common.RoadConnection;
import error.OTMErrorLog;
import error.OTMException;
import geometry.FlowPosition;
import geometry.Side;
import keys.KeyCommPathOrLink;
import common.AbstractLaneGroup;
import packet.PacketLaneGroup;
import utils.OTMUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FluidLaneGroup extends AbstractLaneGroup {

    public float cell_length_meters;

    public double wspeed_cell_per_dt;          // [-]
    public double ffspeed_cell_per_dt;         // [-]
    public double jam_density_veh_per_cell;

    public double capacity_max_veh_per_dt;
    public double capacity_veh_per_dt;

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

    ////////////////////////////////////////////
    // InterfaceLaneGroup
    ///////////////////////////////////////////

    @Override
    public void set_actuator_capacity_vps(float rate_vps) {
        this.capacity_veh_per_dt = rate_vps * ((AbstractFluidModel)link.model).dt_sec;
    }

    @Override
    public void allocate_state() {
        cells.forEach(c -> c.allocate_state());
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

    public final AbstractCell get_upstream_cell(){
        return cells.get(0);
    }

    public final AbstractCell get_dnstream_cell(){
        return cells.get(cells.size()-1);
    }

    public final Map<KeyCommPathOrLink,Double> get_demand(){
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


    public final void create_cells(AbstractFluidModel model,float max_cell_length) throws OTMException {

        // compute cell length
        float r = this.length/max_cell_length;
        boolean is_source_or_sink = link.is_source || link.is_sink;

        int num_cells = is_source_or_sink ?
                1 :
                OTMUtils.approximately_equals(r%1.0,0.0) ? (int) r :  1+((int) r);

        this.cell_length_meters = is_source_or_sink ?
                this.length :
                this.length/num_cells;

        // create the cells
        this.cells = new ArrayList<>();
        for(int i=0;i<num_cells;i++)
            this.cells.add(model.create_cell(this.cell_length_meters, this));

        // designate first and last
        this.cells.get(0).am_upstrm = true;
        this.cells.get(num_cells-1).am_dnstrm = true;
    }
    //////////////////////////////////////////
    // SHOULD THESE BE MOVED TO CTM?
    ///////////////////////////////////////////

    public void release_vehicles(Map<KeyCommPathOrLink,Double> X){
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














