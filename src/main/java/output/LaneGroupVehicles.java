package output;

import common.AbstractLaneGroup;
import error.OTMException;
import runner.Scenario;

import java.util.List;

public class LaneGroupVehicles extends AbstractOutputTimedLanegroup {

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public LaneGroupVehicles(Scenario scenario, String prefix, String output_folder, Long commodity_id, List<Long> link_ids, Float outDt) throws OTMException {
        super(scenario,prefix,output_folder,commodity_id,link_ids,outDt);
        this.type = Type.lanegroup_veh;
    }

    @Override
    public String get_output_file() {
        if(commodity==null)
            return String.format("%s_lanegroup_veh.txt",super.get_output_file());
        else
            return String.format("%s_lanegroup_comm%d_veh.txt",super.get_output_file(),commodity.getId());
    }

    @Override
    protected double get_value_for_lanegroup(Long lg_id){
        if(!lgprofiles.containsKey(lg_id))
            return Double.NaN;
        else {
            AbstractLaneGroup lg = lgprofiles.get(lg_id).lg;
            return lg.vehs_dwn_for_comm(commodity == null ? null : commodity.getId());
        }
    }

}
