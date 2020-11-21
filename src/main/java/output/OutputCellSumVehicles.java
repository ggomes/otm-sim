package output;

import common.Link;
import common.Scenario;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import models.fluid.AbstractFluidModel;
import models.fluid.EventUpdateTotalCellVehicles;
import models.fluid.FluidLaneGroup;
import runner.RunParameters;

import java.util.*;

import static java.util.stream.Collectors.toSet;

public class OutputCellSumVehicles extends AbstractOutputTimedCell {

    public Float simDt;
    public Map<Long, double[] > lg2totals;  // lgid -> cell array of totals

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public OutputCellSumVehicles(Scenario scenario, String prefix, String output_folder, Long commodity_id, Collection<Long> link_ids, Float outDt) throws OTMException {
        super(scenario, prefix, output_folder, commodity_id, link_ids, outDt);

        this.type = Type.cell_sumveh;

        // get the dt
        Set<Link> links = ordered_lgs.stream().map(lg->lg.link).collect(toSet());
        Set<Float> dts = links.stream().map(link->((AbstractFluidModel)link.model).dt_sec).collect(toSet());
        simDt = null;
        if(dts.size()==1)
            this.simDt = dts.iterator().next();

        lg2totals = new HashMap<>();
        for(FluidLaneGroup lg : ordered_lgs)
            lg2totals.put(lg.id,new double[lg.get_num_cells()]);
    }

    @Override
    public void register(RunParameters props, Dispatcher dispatcher) {
        super.register(props, dispatcher); // registers write to files

        // regsister read vehicles event
        dispatcher.register_event(new EventUpdateTotalCellVehicles(dispatcher,props.start_time,this));
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);

        Set<Link> links = ordered_lgs.stream().map(lg->lg.link).collect(toSet());

        // all links must have fluid models
        if(!links.stream().allMatch(link->link.model instanceof AbstractFluidModel)) {
            errorLog.addError("Average vehicles output can only be requested for links with fluid models.");
            return;
        }

        if(simDt==null)
            errorLog.addError("All links in a OutputCellSumVehicles must have the same simulation dt.");
    }

    public void update_total_vehicles(float timestamp){
        Long commid = commodity==null ? null : commodity.getId();
        for(FluidLaneGroup lg : ordered_lgs){
            double[] current_values = lg2totals.get(lg.id);
            for(int i=0;i<lg.cells.size();i++)
                current_values[i] += lg.cells.get(i).get_veh_for_commodity(commid);
        }
    }

    //////////////////////////////////////////////////////
    // InterfaceOutput
    //////////////////////////////////////////////////////

    @Override
    public String get_output_file() {
        return write_to_file ? super.get_output_file() + "_sumveh.txt" : null;
    }

    //////////////////////////////////////////////////////
    // InterfacePlottable
    //////////////////////////////////////////////////////

    @Override
    public String get_yaxis_label() {
        return "veh";
    }

    @Override
    public void plot(String filename) throws OTMException {
        throw new OTMException("Plot not implemented for this output.");
    }

    //////////////////////////////////////////////////////
    // AbstractOutputTimedLanegroup
    //////////////////////////////////////////////////////

    @Override
    protected double[] get_value_for_lanegroup(FluidLaneGroup lg) {
        double [] values = lg2totals.get(lg.id);
        lg2totals.put(lg.id,new double[lg.get_num_cells()]);
        return values;
    }

}
