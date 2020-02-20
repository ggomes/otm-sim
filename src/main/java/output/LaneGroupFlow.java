package output;

import error.OTMException;
import models.AbstractLaneGroup;
import runner.Scenario;
import common.FlowAccumulatorState;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LaneGroupFlow extends AbstractOutputTimedLanegroup  {

    public Map<Long, FlowAccumulatorState> flw_accs;

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public LaneGroupFlow(Scenario scenario, String prefix, String output_folder, Long commodity_id, Collection<Long> link_ids, Float outDt) throws OTMException {
        super(scenario, prefix, output_folder, commodity_id, link_ids, outDt);
        this.type = Type.lanegroup_flw;
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
        flw_accs = new HashMap<>();
        for(LaneGroupProfile lgprofile : lgprofiles.values())
            flw_accs.put(lgprofile.lg.id,lgprofile.lg.request_flow_accumulator(commodity==null ? null : commodity.getId()));
    }

    @Override
    public String get_output_file() {
        if(!write_to_file)
            return null;
        if(commodity==null)
            return String.format("%s_lanegroup_flw.txt",super.get_output_file());
        else
            return String.format("%s_lanegroup_flw_comm%d.txt",super.get_output_file(),commodity.getId());
    }

    @Override
    protected double get_value_for_lanegroup(AbstractLaneGroup lg){
        if(!lgprofiles.containsKey(lg.id))
            return Double.NaN;
        if(commodity==null)
            return flw_accs.get(lg.id).get_total_count();
        else
            return flw_accs.get(lg.id).get_count_for_commodity(commodity.getId());
    }

    @Override
    public String get_yaxis_label() {
        return "flow";
    }
}
