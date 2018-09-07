/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package output;

import error.OTMException;
import common.AbstractLaneGroup;
import common.FlowAccumulator;
import profiles.Profile1D;
import runner.Scenario;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LaneGroupFlow extends AbstractOutputTimedLanegroup {

    // map from lanegroup id to time profile of flow
    Map<Long, Profile1D> flow;

    public List<FlowAccumulator> flowAccumulators;

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public LaneGroupFlow(Scenario scenario,String prefix,String output_folder,Long commodity_id,List<Long> link_ids,Float outDt) throws OTMException {
        super(scenario,prefix,output_folder,commodity_id,link_ids,outDt);
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
        flowAccumulators = new ArrayList<>();
        for(AbstractLaneGroup lanegroup : lanegroups)
            flowAccumulators.add(lanegroup.request_flow_accumulator(commodity==null ? null : commodity.getId()));
    }

    @Override
    public String get_output_file() {
        return super.get_output_file() + "_lanegroup_flw.txt";
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
                    FlowAccumulator fa = flowAccumulators.get(i);
                    if(!isfirst)
                        writer.write(AbstractOutputTimed.delim);
                    isfirst = false;
                    writer.write(String.format("%f",fa.vehicle_count));
                }
                writer.write("\n");
            } catch (IOException e) {
                throw new OTMException(e);
            }
        } else {
            for(int i=0;i<lanegroups.size();i++) {
                FlowAccumulator fa = flowAccumulators.get(i);
                flow.get(lanegroups.get(i).id).add(fa.vehicle_count);
            }
        }

    }


}
