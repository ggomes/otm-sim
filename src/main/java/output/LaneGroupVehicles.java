package output;

import models.BaseLaneGroup;
import error.OTMException;
import runner.Scenario;

import java.util.Collection;

public class LaneGroupVehicles extends AbstractOutputTimedLanegroup {

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public LaneGroupVehicles(Scenario scenario, String prefix, String output_folder, Long commodity_id, Collection<Long> link_ids, Float outDt) throws OTMException {
        super(scenario,prefix,output_folder,commodity_id,link_ids,outDt);
        this.type = Type.lanegroup_veh;
    }

    @Override
    public String get_output_file() {
        if(!write_to_file)
            return null;
        if(commodity==null)
            return String.format("%s_lanegroup_veh.txt",super.get_output_file());
        else
            return String.format("%s_lanegroup_comm%d_veh.txt",super.get_output_file(),commodity.getId());
    }

    @Override
    protected double get_value_for_lanegroup(BaseLaneGroup lg){
        if(!lgprofiles.containsKey(lg.id))
            return Double.NaN;
        else {
            return lg.vehs_dwn_for_comm(commodity == null ? null : commodity.getId());
        }
    }

}
