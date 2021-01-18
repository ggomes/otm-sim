package output;

import cmd.RunParameters;
import core.AbstractFluidModel;
import core.AbstractLaneGroup;
import core.Link;
import core.Scenario;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import models.fluid.EventUpdateTotalLanegroupVehicles;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class OutputLaneGroupSumVehiclesDwn  extends AbstractOutputTimedLanegroup  {

    public Float simDt;
    public Map<Long,Float> lg2total;

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public OutputLaneGroupSumVehiclesDwn(Scenario scenario, String prefix, String output_folder, Long commodity_id, Collection<Long> inlink_ids, Float outDt) throws OTMException {
        super(scenario, prefix, output_folder, commodity_id, inlink_ids, outDt);
        this.type = Type.lanegroup_sumvehdwn;
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);

        lg2total = new HashMap<>();
        for(Long lgid : lgprofiles.keySet())
            lg2total.put(lgid,0f);

        // get the dt
        Set<Link> links = lgprofiles.values().stream().map(lgp->lgp.lg.get_link()).collect(toSet());
        Set<Float> dts = links.stream().map(link->((AbstractFluidModel)link.get_model()).dt_sec).collect(toSet());
        simDt = null;
        if(dts.size()==1)
            this.simDt = dts.iterator().next();

    }

    @Override
    public void register(RunParameters props, Dispatcher dispatcher) {
        super.register(props, dispatcher); // registers write to files

        // regsister read vehicles event
        dispatcher.register_event(new EventUpdateTotalLanegroupVehicles(dispatcher,props.start_time,this));
    }

    @Override
    public void validate_post_init(OTMErrorLog errorLog) {
        super.validate_post_init(errorLog);

        Set<Link> links = lgprofiles.values().stream().map(lgp->lgp.lg.get_link()).collect(toSet());

        // all links must have fluid models
        if(!links.stream().allMatch(link->link.get_model() instanceof AbstractFluidModel)) {
            errorLog.addError("Average vehicles output can only be requested for links with fluid models.");
            return;
        }

        if(simDt==null)
            errorLog.addError("All links  in a OutputLaneGroupAvgVehicles must have the same simulation dt.");
    }

    public void update_total_vehicles(float timestamp){
        Long commid = commodity==null ? null : commodity.getId();
        for(AbstractLaneGroup lg : ordered_lgs){
            float current_value = lg2total.get(lg.getId());
            lg2total.put(lg.getId(),current_value + lg.vehs_dwn_for_comm(commid));
        }
    }

    //////////////////////////////////////////////////////
    // InterfaceOutput
    //////////////////////////////////////////////////////

    @Override
    public String get_output_file() {
        return write_to_file ? super.get_output_file() + "_sumvehdwn.txt" : null;
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
    protected double get_value_for_lanegroup(AbstractLaneGroup lg) {
        double value = lg2total.get(lg.getId());
        lg2total.put(lg.getId(),0f);
        return value;
    }

}
