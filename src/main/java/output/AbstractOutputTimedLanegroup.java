/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package output;

import error.OTMErrorLog;
import error.OTMException;
import common.AbstractLaneGroup;
import runner.Scenario;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AbstractOutputTimedLanegroup extends AbstractOutputTimed {

    public List<AbstractLaneGroup> lanegroups;

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////


    public AbstractOutputTimedLanegroup(Scenario scenario,String prefix,String output_folder,Long commodity_id,List<Long> link_ids,Float outDt) throws OTMException
    {
        super(scenario,prefix,output_folder,commodity_id,outDt);

        // get lanegroup list
        lanegroups = new ArrayList<>();
        if(link_ids==null)
            lanegroups.addAll(scenario.network.get_lanegroups());
        else
            for(Long link_id : link_ids)
                if(scenario.network.links.containsKey(link_id))
                   lanegroups.addAll(scenario.network.links.get(link_id).lanegroups.values());
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);

        if(lanegroups.isEmpty())
            errorLog.addError("no lanegroups in output request");
    }

    //////////////////////////////////////////////////////
    // write lanegroups
    //////////////////////////////////////////////////////

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);

        if(write_to_file){
            try {
                String filename = get_output_file();
                if(filename!=null) {
                    String subfilename = filename.substring(0,filename.length()-4);
                    Writer lanegroups_writer = new OutputStreamWriter(new FileOutputStream(subfilename + "_lanegroups.txt"));
                    for(AbstractLaneGroup lg : lanegroups)
                        lanegroups_writer.write(lg.id+" "+lg.link.getId() + " " + Collections.min(lg.lanes) + " " + Collections.max(lg.lanes) +"\n");
                    lanegroups_writer.close();
                }
            } catch (FileNotFoundException exc) {
                throw new OTMException(exc);
            } catch (IOException e) {
                throw new OTMException(e);
            }
        }

    }

}
