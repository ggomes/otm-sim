/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package output;

import common.AbstractLaneGroup;
import dispatch.Dispatcher;
import error.OTMException;
import runner.RunParameters;
import runner.Scenario;
import utils.OTMUtils;

import java.io.IOException;
import java.util.ArrayList;

public class LaneGroups extends AbstractOutput {

    public LaneGroups(Scenario scenario,String prefix,String output_folder) throws OTMException {
        super(scenario,prefix,output_folder);
    }

    @Override
    public String get_output_file() {
        return super.get_output_file() + "_lanegroups.txt";
    }


    @Override
    public void write(float timestamp, Object obj) throws OTMException {
        System.out.println("this should not happen");
    }

    @Override
    public void register(RunParameters props, Dispatcher dispatcher) {
        if(writer==null)
            return;
        try {
            for(AbstractLaneGroup lg : scenario.network.get_lanegroups())
                writer.write(lg.id + "\t" + lg.link.getId() + "\t{" + OTMUtils.comma_format(new ArrayList(lg.lanes)) + "}\n");
            writer.close();
            writer = null;
        } catch (IOException e) {
            return;
        }
    }

}
