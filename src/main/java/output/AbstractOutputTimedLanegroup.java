/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package output;

import common.Link;
import error.OTMErrorLog;
import error.OTMException;
import common.AbstractLaneGroup;
import profiles.Profile1D;
import runner.Scenario;

import java.io.*;
import java.util.*;

public abstract class AbstractOutputTimedLanegroup extends AbstractOutputTimed {

    public List<Long> ordered_ids;
    public Map<Long, LankGroupProfile> lgprofiles;

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public AbstractOutputTimedLanegroup(Scenario scenario,String prefix,String output_folder,Long commodity_id,List<Long> link_ids,Float outDt) throws OTMException {
        super(scenario,prefix,output_folder,commodity_id,outDt);

        // get lanegroup list
        if(link_ids==null)
            link_ids = new ArrayList<>(scenario.network.links.keySet());

        ordered_ids = new ArrayList<>();
        lgprofiles = new HashMap<>();
        for(Long link_id : link_ids){
            if(!scenario.network.links.containsKey(link_id))
                continue;
            Link link = scenario.network.links.get(link_id);
            for(AbstractLaneGroup lg : link.lanegroups.values()){
                ordered_ids.add(lg.id);
                lgprofiles.put(lg.id, new LankGroupProfile(lg));
            }
        }

    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);

        if(lgprofiles.isEmpty())
            errorLog.addError("no lanegroups in output request");
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);

        if(write_to_file){
            try {
                String filename = get_output_file();
                if(filename!=null) {
                    String subfilename = filename.substring(0,filename.length()-4);
                    Writer lanegroups_writer = new OutputStreamWriter(new FileOutputStream(subfilename + "_lanegroups.txt"));
                    for(LankGroupProfile lgprof: lgprofiles.values()){
                        AbstractLaneGroup lg = lgprof.lg;
                        lanegroups_writer.write(lg.id+" "+lg.link.getId() + " " + Collections.min(lg.lanes) + " " + Collections.max(lg.lanes) +"\n");
                    }
                    lanegroups_writer.close();
                }
            } catch (FileNotFoundException exc) {
                throw new OTMException(exc);
            } catch (IOException e) {
                throw new OTMException(e);
            }
        } else {
            for(LankGroupProfile lgProfile : lgprofiles.values())
                lgProfile.initialize(outDt);
        }

    }

    //////////////////////////////////////////////////////
    // get / plot
    //////////////////////////////////////////////////////

    abstract protected double get_value_for_lanegroup(Long lg_id);

    public Map<Long,Profile1D> get_profiles_for_linkid(Long link_id){

        if(!scenario.network.links.containsKey(link_id))
            return null;
        if(write_to_file)
            return null;

        Map<Long,Profile1D> profiles = new HashMap<>();
        for(AbstractLaneGroup lg : scenario.network.links.get(link_id).lanegroups.values())
            if(lgprofiles.containsKey(lg.id))
                profiles.put(lg.id,lgprofiles.get(lg.id).profile);

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
                for(Long lg_id : ordered_ids){
                    if(!isfirst)
                        writer.write(AbstractOutputTimed.delim);
                    isfirst = false;
                    writer.write(String.format("%f",get_value_for_lanegroup(lg_id)));
                }
                writer.write("\n");
            } catch (IOException e) {
                throw new OTMException(e);
            }
        } else {
            for(Long lg_id : ordered_ids){
                LankGroupProfile lgProfile = lgprofiles.get(lg_id);
                lgProfile.add_value(get_value_for_lanegroup(lg_id));
            }
        }
    }

    //////////////////////////////////////////////////////
    // class
    //////////////////////////////////////////////////////

    public class LankGroupProfile {
        public AbstractLaneGroup lg;
        public Profile1D profile;
        public LankGroupProfile(AbstractLaneGroup lg){
            this.lg = lg;
        }
        public void initialize(float outDt){
            this.profile = new Profile1D(null, outDt);
        }
        public void add_value(double value){
            profile.add(value);
        }
    }
}
