package output;

import common.AbstractLaneGroup;
import error.OTMException;
import common.Scenario;

import java.util.Collection;

public class OutputLaneGroupVehicles extends AbstractOutputTimedLanegroup {

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public OutputLaneGroupVehicles(Scenario scenario, String prefix, String output_folder, Long commodity_id, Collection<Long> link_ids, Float outDt) throws OTMException {
        super(scenario,prefix,output_folder,commodity_id,link_ids,outDt);
        this.type = Type.lanegroup_veh;
    }

    //////////////////////////////////////////////////////
    // InterfaceOutput
    //////////////////////////////////////////////////////

    @Override
    public String get_output_file() {
        if(!write_to_file)
            return null;
        if(commodity==null)
            return String.format("%s_lanegroup_veh.txt",super.get_output_file());
        else
            return String.format("%s_lanegroup_comm%d_veh.txt",super.get_output_file(),commodity.getId());
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
        throw new OTMException("Plot not implemented for LaneGroupVehicles output.");
    }

    //////////////////////////////////////////////////////
    // AbstractOutputTimedLanegroup
    //////////////////////////////////////////////////////

    @Override
    protected double get_value_for_lanegroup(AbstractLaneGroup lg){
        if(!lgprofiles.containsKey(lg.id))
            return Double.NaN;
        else {
            return lg.get_total_vehicles_for_commodity(commodity == null ? null : commodity.getId());
        }
    }

}
