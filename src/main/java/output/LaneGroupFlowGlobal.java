package output;

import common.AbstractLaneGroup;
import error.OTMException;
import profiles.Profile1D;
import runner.Scenario;
import sensor.FlowAccumulatorGlobal;

import java.io.IOException;
import java.util.*;

public class LaneGroupFlowGlobal extends AbstractOutputTimedLanegroup  {

    // map from lanegroup id to time profile of flow
    private Map<Long, Profile1D> flow;

    public List<FlowAccumulatorGlobal> flw_accs;

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public LaneGroupFlowGlobal(Scenario scenario, String prefix, String output_folder, List<Long> link_ids, Float outDt) throws OTMException {
        super(scenario, prefix, output_folder, null, link_ids, outDt);
        this.type = Type.lanegroup_flw;

        if(!write_to_file) {
            flow = new HashMap<>();
            for(AbstractLaneGroup lg : lanegroups)
                flow.put(lg.id,new Profile1D(null,outDt));
        }
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
        flw_accs = new ArrayList<>();
        for(AbstractLaneGroup lanegroup : lanegroups)
            flw_accs.add(lanegroup.request_flow_accumulator());
    }

    @Override
    public String get_output_file() {
        return super.get_output_file() + "_lanegroup_flw_total.txt";
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

        if(write_to_file){
            super.write(timestamp,null);
            try {
                boolean isfirst=true;
                for(int i=0;i<lanegroups.size();i++){
                    FlowAccumulatorGlobal fa = flw_accs.get(i);
                    if(!isfirst)
                        writer.write(AbstractOutputTimed.delim);
                    isfirst = false;
                    writer.write(String.format("%f",fa.count));
                }
                writer.write("\n");
            } catch (IOException e) {
                throw new OTMException(e);
            }
        } else {
            for(int i=0;i<lanegroups.size();i++) {
                FlowAccumulatorGlobal fa = flw_accs.get(i);
                flow.get(lanegroups.get(i).id).add(fa.count);
            }
        }

    }


}
