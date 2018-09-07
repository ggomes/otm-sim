/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package output;

import error.OTMException;
import common.*;
import org.jfree.data.xy.XYSeries;
import profiles.Profile1D;
import runner.Scenario;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinkFlow extends AbstractOutputTimedLink {

    private Map<Long,FlowAccumulatorSet> flow_accumulator_sets;

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public LinkFlow(Scenario scenario,String prefix,String output_folder,Long commodity_id,List<Long> link_ids,Float outDt) throws OTMException {
        super(scenario,prefix,output_folder,commodity_id,link_ids,outDt);
        this.type = Type.link_flw;
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
        flow_accumulator_sets = new HashMap<>();
        for(Link link : links.values())
            flow_accumulator_sets.put(link.getId(),link.request_flow_accumulator_set(commodity==null ? null : commodity.getId()));
    }

    @Override
    public String get_output_file() {
        return super.get_output_file() + "_link_flw.txt";
    }

    @Override
    String get_yaxis_label() {
        return "Flow [veh/hr]";
    }

    @Override
    XYSeries get_series_for_linkid(Long link_id) {
        Profile1D profile = values.get(link_id).clone();
        profile.multiply(3600d/outDt);
        return profile.get_series(String.format("%d",link_id));
    }

    //////////////////////////////////////////////////////
    // get
    //////////////////////////////////////////////////////

    public List<Double> get_flow_for_link_in_vph(Long link_id){
        if(!values.containsKey(link_id))
            return null;
        Profile1D profile = values.get(link_id).clone();
        profile.multiply(3600d/outDt);
        return profile.get_values();
    }

    public double get_flow_vph_for_linkid_timestep(Long link_id,int timestep) throws OTMException {
        Profile1D profile = get_profile_for_linkid(link_id);
        if(profile==null)
            throw new OTMException("Bad link id in get_flow_vph_for_linkid_timestep()");
        if(timestep<0 || timestep>=profile.values.size())
            throw new OTMException("Bad timestep in get_flow_vph_for_linkid_timestep()");
        return 3600*(profile.get_ith_value(timestep+1)-profile.get_ith_value(timestep))/profile.dt;
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
                for(FlowAccumulatorSet fas : flow_accumulator_sets.values()){
                    if(!isfirst)
                        writer.write(AbstractOutputTimed.delim);
                    isfirst = false;
                    writer.write(String.format("%f",fas.get_vehicle_count()));
                    fas.reset();
                }
                writer.write("\n");
            } catch (IOException e) {
                throw new OTMException(e);
            }
        } else {
            for(Long link_id : links.keySet()){
                FlowAccumulatorSet fas = flow_accumulator_sets.get(link_id);
                values.get(link_id).add(fas.get_vehicle_count());
                fas.reset();
            }
        }

    }

}
