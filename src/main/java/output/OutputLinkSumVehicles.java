package output;

import common.Scenario;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import models.fluid.AbstractFluidModel;
import models.fluid.EventUpdateTotalLinkVehicles;
import runner.RunParameters;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class OutputLinkSumVehicles  extends AbstractOutputTimedLink {

    public Float simDt;
    public Map<Long,Double> totals;

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public OutputLinkSumVehicles(Scenario scenario, String prefix, String output_folder, Long commodity_id, Collection<Long> link_ids, Float outDt) throws OTMException {
        super(scenario, prefix, output_folder, commodity_id, link_ids, outDt);
        this.type = Type.link_sumveh;

        // get the dt
        try {
            Set<Float> dts = linkprofiles.values().stream().map(x -> ((AbstractFluidModel) x.link.model).dt_sec).collect(toSet());
            simDt = null;
            if(dts.size()==1)
                this.simDt = dts.iterator().next();
        } catch (Exception e){
            throw new OTMException("Could not assign dt for sum vehicles output");
        }

        totals = new HashMap<>();
        for(Long linkid : ordered_ids)
            totals.put(linkid,0d);

    }

    @Override
    public void register(RunParameters props, Dispatcher dispatcher) {
        super.register(props, dispatcher); // registers write to files

        // regsister read vehicles event
        dispatcher.register_event(new EventUpdateTotalLinkVehicles(dispatcher,props.start_time,this));
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);

        // all links must have fluid models
        if(!linkprofiles.values().stream().allMatch(x->x.link.model instanceof AbstractFluidModel)) {
            errorLog.addError("Sum vehicles output can only be requested for links with fluid models.");
            return;
        }

        if(simDt==null)
            errorLog.addError("All links in a OutputLinkSumVehicles must have the same simulation dt.");
    }

    public void update_total_vehicles(float timestamp){
        Long commid = commodity==null ? null : commodity.getId();
        for(long linkid : ordered_ids)
            totals.put(linkid,totals.get(linkid) + linkprofiles.get(linkid).link.get_veh_for_commodity(commid)) ;
    }

    //////////////////////////////////////////////////////
    // InterfaceOutput
    //////////////////////////////////////////////////////

    @Override
    public String get_output_file() {
        if(!write_to_file)
            return null;
        return super.get_output_file() + "_sumveh.txt";
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

    @Override
    public double get_value_for_link(Long link_id) {
        double value = totals.get(link_id);
        totals.put(link_id,0d);
        return value;
    }

}
