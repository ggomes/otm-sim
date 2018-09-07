/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package output;

import error.OTMException;
import common.AbstractLaneGroup;
import profiles.Profile1D;
import runner.Scenario;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LaneGroupVehicles extends AbstractOutputTimedLanegroup {

    // map from lanegroup id to time profile of vehicles
    public Map<Long, Profile1D> vehicles;

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public LaneGroupVehicles(Scenario scenario, String prefix, String output_folder, Long commodity_id, List<Long> link_ids, Float outDt) throws OTMException {
        super(scenario,prefix,output_folder,commodity_id,link_ids,outDt);
        this.type = Type.lanegroup_veh;

        if(!write_to_file) {
            vehicles = new HashMap<>();
            for(AbstractLaneGroup lg : lanegroups)
                vehicles.put(lg.id,new Profile1D(null,outDt));

        }
    }

    @Override
    public String get_output_file() {
        return super.get_output_file() + "_lanegroup_veh.txt";
    }

    public Map<Long,Profile1D> get_profiles_for_linkid(Long link_id){

        if(!scenario.network.links.containsKey(link_id))
            return null;

        Map<Long,Profile1D> profiles = new HashMap<>();
        for(AbstractLaneGroup lg : scenario.network.links.get(link_id).lanegroups.values())
            if(vehicles.containsKey(lg.id))
                profiles.put(lg.id,vehicles.get(lg.id));

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
                for(AbstractLaneGroup lg : lanegroups){
                    if(!isfirst)
                        writer.write(AbstractOutputTimed.delim);
                    isfirst = false;
                    writer.write(String.format("%f",lg.vehicles_for_commodity(commodity==null?null:commodity.getId())));
                }
                writer.write("\n");
            } catch (IOException e) {
                throw new OTMException(e);
            }
        } else {
            for(AbstractLaneGroup lg : lanegroups)
                vehicles.get(lg.id).add(lg.vehicles_for_commodity(commodity==null?null:commodity.getId()));
        }
    }

}
