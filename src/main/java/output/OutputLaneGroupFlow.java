package output;

import error.OTMException;
import core.AbstractLaneGroup;
import core.Scenario;
import core.FlowAccumulatorState;
import org.jfree.data.xy.XYSeries;
import profiles.Profile1D;
import utils.OTMUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class OutputLaneGroupFlow extends AbstractOutputTimedLanegroup {

    private Map<Long, FlowAccumulatorState> flw_accs;   // lg id -> acc

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public OutputLaneGroupFlow(Scenario scenario, String prefix, String output_folder, Long commodity_id, Collection<Long> link_ids, Float outDt) throws OTMException {
        super(scenario, prefix, output_folder, commodity_id, link_ids, outDt);
        this.type = Type.lanegroup_flw;
    }

    //////////////////////////////////////////////////////
    // InterfaceOutput
    //////////////////////////////////////////////////////

    @Override
    public String get_output_file() {
        return write_to_file ? super.get_output_file() + "_flw.txt" : null;
    }

    //////////////////////////////////////////////////////
    // AbstractOutput
    //////////////////////////////////////////////////////

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
        flw_accs = new HashMap<>();
        for(LaneGroupProfile lgprofile : lgprofiles.values())
            flw_accs.put(lgprofile.lg.getId(),lgprofile.lg.request_flow_accumulator(commodity==null ? null : OTMUtils.hashset(commodity.getId())));
    }

    //////////////////////////////////////////////////////
    // InterfacePlottable
    //////////////////////////////////////////////////////

    @Override
    public String get_yaxis_label() {
        return "flow";
    }

    @Override
    public void plot(String filename) throws OTMException {
        throw new OTMException("Plot not implemented for LaneGroupFlow output.");
    }

    //////////////////////////////////////////////////////
    // AbstractOutputTimedLanegroup
    //////////////////////////////////////////////////////

    @Override
    protected double get_value_for_lanegroup(AbstractLaneGroup lg){
        if(!lgprofiles.containsKey(lg.getId()))
            return Double.NaN;
        if(commodity==null)
            return flw_accs.get(lg.getId()).get_total_count();
        else
            return flw_accs.get(lg.getId()).get_count_for_commodity(commodity.getId());
    }

    @Override
    public XYSeries get_series_for_lg(AbstractLaneGroup lg) {
        if(!lgprofiles.containsKey(lg.getId()))
            return null;
        return get_flow_profile_for_lg_in_vph(lg.getId()).get_series(String.format("%d (%d-%d)",lg.get_link().getId(),lg.get_start_lane_dn(),lg.get_start_lane_dn()+lg.get_num_lanes()-1));
    }

    public Profile1D get_flow_profile_for_lg_in_vph(Long lgid){
        Profile1D profile = lgprofiles.get(lgid).profile;
        return new Profile1D(profile.start_time,profile.dt,profile.difftimes(3600d/outDt));
    }

}
