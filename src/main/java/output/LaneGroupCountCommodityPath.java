package output;

import common.AbstractLaneGroup;
import error.OTMException;
import profiles.Profile1D;
import runner.Scenario;
import sensor.AccumulatorCommodity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LaneGroupCountCommodityPath extends AbstractOutputTimedLanegroup  {

    // map from lanegroup id to time profile of flow
    Map<Long, Profile1D> flow;

    public List<AccumulatorCommodity> flw_accs;

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public LaneGroupCountCommodityPath(Scenario scenario, String prefix, String output_folder, Long commodity_id, Long path_id, Float outDt) throws OTMException {
        super(scenario, prefix, output_folder, commodity_id, null, outDt); // TOD FIX THIS
//        this.type = Type.lanegroup_flw;
//
//        if(!write_to_file) {
//            flow = new HashMap<>();
//            for(AbstractLaneGroup lg : lanegroups)
//                flow.put(lg.id,new Profile1D(null,outDt));
//        }
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
        flw_accs = new ArrayList<>();
        for(AbstractLaneGroup lanegroup : lanegroups)
            flw_accs.add(lanegroup.request_flow_accumulator(commodity.getId()));
    }

    @Override
    public String get_output_file() {
        return String.format("%s_lanegroup_flw_comm%d.txt",super.get_output_file(),commodity.getId());
    }

    public Map<Long,Profile1D> get_profiles_for_linkid(Long link_id){

        if(!scenario.network.links.containsKey(link_id))
            return null;

        Map<Long,Profile1D> profiles = new HashMap<>();
        for(AbstractLaneGroup lg : scenario.network.links.get(link_id).lanegroups.values())
            if(flow.containsKey(lg.id))
                profiles.put(lg.id,flow.get(lg.id));

        return profiles;
    }

    //////////////////////////////////////////////////////
    // write
    //////////////////////////////////////////////////////

    @Override
    public void write(float timestamp,Object obj) throws OTMException {

        throw new OTMException("LaneGroupCountCommodityPath implementation is incomplete.");

//        if(write_to_file){
//            super.write(timestamp,null);
//            try {
//                boolean isfirst=true;
//                for(int i=0;i<lanegroups.size();i++){
//                    AccumulatorCommodity fa = flw_accs.get(i);
//                    if(!isfirst)
//                        writer.write(AbstractOutputTimed.delim);
//                    isfirst = false;
//                    writer.write(String.format("%f",fa.count));     // TODO FIX THIS
//                }
//                writer.write("\n");
//            } catch (IOException e) {
//                throw new OTMException(e);
//            }
//        } else {
//            for(int i=0;i<lanegroups.size();i++) {
//                AccumulatorCommodity fa = flw_accs.get(i);
//                flow.get(lanegroups.get(i).id).add(fa.count);     // TODO FIX THIS
//            }
//        }

    }


}