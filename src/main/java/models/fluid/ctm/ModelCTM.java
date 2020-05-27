package models.fluid.ctm;

import common.Link;

import error.OTMErrorLog;
import error.OTMException;
import geometry.Side;
import jaxb.OutputRequest;
import keys.KeyCommPathOrLink;
import common.AbstractLaneGroup;
import models.fluid.*;
import output.AbstractOutput;
import common.Scenario;
import traveltime.FluidLaneGroupTimer;
import utils.OTMUtils;
import utils.StochasticProcess;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ModelCTM extends AbstractFluidModel {

    public ModelCTM(String name, boolean is_default, StochasticProcess process, jaxb.ModelParams param) {
        super(name,is_default,param.getSimDt()==null ? -1 : param.getSimDt(),process,param.getMaxCellLength());
    }

    //////////////////////////////////////////////////////////////
    // InterfaceModel
    //////////////////////////////////////////////////////////////

    @Override
    public void validate(OTMErrorLog errorLog) {

    }

    @Override
    public AbstractOutput create_output_object(Scenario scenario, String prefix, String output_folder, OutputRequest jaxb_or)  throws OTMException {
        AbstractOutput output = null;
        switch (jaxb_or.getQuantity()) {
            case "cell_veh":
                Long commodity_id = jaxb_or.getCommodity();
                Float outDt = jaxb_or.getDt();
                output = new OutputCellVehicles(scenario, this,prefix, output_folder, commodity_id, outDt);
                break;
            default:
                throw new OTMException("Bad output identifier : " + jaxb_or.getQuantity());
        }
        return output;
    }

    @Override
    public Map<AbstractLaneGroup,Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups) {
        Map<AbstractLaneGroup,Double> A = new HashMap<>();
        double total_supply = candidate_lanegroups.stream().mapToDouble(x->x.get_supply()).sum();
        for(AbstractLaneGroup laneGroup : candidate_lanegroups)
            A.put(laneGroup , laneGroup.get_supply() / total_supply);
        return A;
    }

    //////////////////////////////////////////////////////////////
    // InterfaceFluidModel
    //////////////////////////////////////////////////////////////

    @Override
    public void compute_lanechange_demand_supply(Link link, float timestamp) throws OTMException {

        // TODO: should update_flux I and II be passed the link as in update_state?
        // TODO What is the point of that?

        // TODO cache this?
        update_supply_for_all_cells(link,timestamp);

        if(link.lanegroups_flwdn.size()>=2)
            perform_lane_changes(link,timestamp);

        update_demand(link,timestamp);

    }

    @Override
    public void update_link_state(Link link,float timestamp) throws OTMException {

        for(AbstractLaneGroup alg : link.lanegroups_flwdn.values()) {

            FluidLaneGroup lg = (FluidLaneGroup) alg;

            if(lg.states.isEmpty())
                continue;

            double total_travel_time = 0d;

            for(int i=0;i<lg.cells.size()-1;i++) {

                CTMCell upcell = (CTMCell) lg.cells.get(i);
                CTMCell dncell = (CTMCell) lg.cells.get(i + 1);

                Map<KeyCommPathOrLink, Double> dem_dwn = upcell.demand_dwn;
                Map<KeyCommPathOrLink, Double> dem_out = upcell.demand_out;
                Map<KeyCommPathOrLink, Double> dem_in = upcell.demand_in;

                // total demand
                double total_demand = OTMUtils.sum(dem_dwn);
                total_demand += dem_out == null ? 0d : OTMUtils.sum(dem_out);
                total_demand += dem_in == null ? 0d : OTMUtils.sum(dem_in);

                if (total_demand > OTMUtils.epsilon) {
                    double total_flow = Math.min(total_demand, dncell.supply);
                    double gamma = total_flow / total_demand;

                    Map<KeyCommPathOrLink, Double> flow_stay = OTMUtils.times(dem_dwn, gamma);
                    Map<KeyCommPathOrLink, Double> flow_lc_in = OTMUtils.times(dem_in, gamma);
                    Map<KeyCommPathOrLink, Double> flow_lc_out = OTMUtils.times(dem_out, gamma);

                    // travel time computation
                    if(lg.travel_timer!=null){
                        double veh = upcell.get_vehicles();
                        double tt;
                        if(veh>0) {

                            double out_flow = flow_stay==null ? 0d : flow_stay.values().stream().mapToDouble(x->x).sum();

                            if(out_flow==0)
                                tt = link.is_source ? dt_sec : dt_sec / lg.ffspeed_cell_per_dt;
                            else
                                tt = dt_sec * veh / out_flow;

                        } else
                            tt = link.is_source ? dt_sec : dt_sec / lg.ffspeed_cell_per_dt;
                        total_travel_time += tt;
                    }

                    dncell.add_vehicles(flow_stay,flow_lc_in,flow_lc_out);
                    upcell.subtract_vehicles(flow_stay,flow_lc_in,flow_lc_out);
                }

            }

            // travel time computation
            if(lg.travel_timer!=null)
                ((FluidLaneGroupTimer)lg.travel_timer).add_sample(total_travel_time);

            lg.update_supply();

            // process buffer
            if(link.is_model_source_link) {
                lg.process_buffer(timestamp);
                lg.update_supply();
            }

        }
    }

    @Override
    public AbstractCell create_cell(FluidLaneGroup lg) throws OTMException {
        return new CTMCell(lg);
    }

    ///////////////////////////////////////////
    // private
    ///////////////////////////////////////////

    private void perform_lane_changes(Link link,float timestamp) {

        // WARNING: THIS ASSUMES ONLY FULL LENGTH ADDLANES

//        REWRITE THIS !!!!!

        int cells_in_full_lg = ((FluidLaneGroup)link.lanegroups_flwdn.values().iterator().next()).cells.size();

        // scan cross section from upstream to downstream
        for (int i = 0; i < cells_in_full_lg; i++) {

            // compute total flows reduction for each lane group
            Map<Long, Double> gamma = new HashMap<>();
            for (AbstractLaneGroup alg : link.lanegroups_flwdn.values()) {
                FluidLaneGroup lg = (FluidLaneGroup) alg;
                CTMCell cell = (CTMCell) lg.cells.get(i);
                double demand_to_me = 0d;
                if (lg.neighbor_in!=null) {
                    CTMCell ncell = (CTMCell) ((FluidLaneGroup) lg.neighbor_in).cells.get(i);
                    if(!ncell.out_barrier)
                        demand_to_me += ncell.total_vehs_out;
                }
                if (lg.neighbor_out != null) {
                    CTMCell ncell = (CTMCell) ((FluidLaneGroup) lg.neighbor_out).cells.get(i);
                    if(!ncell.in_barrier)
                        demand_to_me += ncell.total_vehs_in;
                }

                // TODO: extract xi as a parameter
                double supply = 0.9d * (1d - lg.wspeed_cell_per_dt) * cell.supply;
                gamma.put(lg.id, demand_to_me > supply ? supply / demand_to_me : 1d);
            }

            // lane change flow
            // WARNING: This assumes that no state has vehicles going in both directions.
            // ie a flow that goes left does not also go right. Otherwise I think there may
            // be "data races", where the result depends on the order of lgs.
            for (AbstractLaneGroup alg : link.lanegroups_flwdn.values()) {
                FluidLaneGroup to_lg = (FluidLaneGroup) alg;
                CTMCell to_cell = (CTMCell) to_lg.cells.get(i);
                double my_gamma = gamma.get(to_lg.id);

                if (to_lg.neighbor_in != null) {
                    FluidLaneGroup from_lg = (FluidLaneGroup) to_lg.neighbor_in;
                    CTMCell ncell = (CTMCell) from_lg.cells.get(i);
                    if(!ncell.out_barrier)
                        ncell.total_vehs_out -= do_lane_changes(to_lg,to_cell,my_gamma,ncell.veh_out);
                }

                if (to_lg.neighbor_out != null) {
                    FluidLaneGroup from_lg = (FluidLaneGroup) to_lg.neighbor_out;
                    CTMCell ncell = (CTMCell) from_lg.cells.get(i);
                    if(!ncell.in_barrier)
                        ncell.total_vehs_in -= do_lane_changes(to_lg,to_cell,my_gamma,ncell.veh_in);
                }
            }
        }

    }

    // call update_supply_demand on each cell
    private void update_supply_for_all_cells(Link link,float timestamp) {
        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values()) {
            FluidLaneGroup ctmlg = (FluidLaneGroup) lg;
            if(!ctmlg.states.isEmpty())
                ctmlg.cells.forEach(cell -> cell.update_supply());
        }
    }

    private void update_demand(Link link,float timestamp) {
        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values()) {
            FluidLaneGroup ctmlg = (FluidLaneGroup) lg;
            if(!ctmlg.states.isEmpty())
                ctmlg.cells.forEach(cell -> cell.update_demand());
        }
    }

    private double do_lane_changes(FluidLaneGroup to_lg,CTMCell to_cell,double to_gamma,Map<KeyCommPathOrLink, Double> from_vehs){
        double total_flw = 0d;
        for (Map.Entry<KeyCommPathOrLink, Double> e : from_vehs.entrySet()) {
            Double from_veh = e.getValue();
            KeyCommPathOrLink state = e.getKey();

            if (from_veh > OTMUtils.epsilon) {

                double flw = to_gamma * from_veh;

                // remove from this cell
                from_vehs.put(state, from_veh-flw );
                total_flw += flw;

                // add to side cell
                Side newside = to_lg.state2lanechangedirection.containsKey(state) ?
                        to_lg.state2lanechangedirection.get(state) :
                        Side.middle;
                switch (newside) {
                    case in:
                        to_cell.veh_in.put(state, to_cell.veh_in.get(state) + flw);
                        to_cell.total_vehs_in += flw;
                        break;
                    case middle:
                        to_cell.veh_dwn.put(state, to_cell.veh_dwn.get(state) + flw);
                        to_cell.total_vehs_dwn += flw;
                        break;
                    case out:
                        to_cell.veh_out.put(state, to_cell.veh_out.get(state) + flw);
                        to_cell.total_vehs_out += flw;
                        break;
                }
            }
        }
        return total_flw;
    }

}
