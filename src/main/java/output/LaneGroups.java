package output;

import models.BaseLaneGroup;
import dispatch.Dispatcher;
import error.OTMException;
import runner.RunParameters;
import runner.Scenario;
import utils.OTMUtils;

import java.io.IOException;

public class LaneGroups extends AbstractOutput {

    public LaneGroups(Scenario scenario,String prefix,String output_folder) throws OTMException {
        super(scenario,prefix,output_folder);
    }

    @Override
    public String get_output_file() {
        return write_to_file ? super.get_output_file() + "_lanegroups.txt" : null;
    }

    @Override
    public void write(float timestamp, Object obj) {
        System.err.println("this should not happen");
    }

    @Override
    public void register(RunParameters props, Dispatcher dispatcher) {
        if(writer==null)
            return;
        try {
            for(BaseLaneGroup lg : scenario.network.get_lanegroups())
                writer.write(lg.id + "\t" + lg.link.getId() + "\t{" + OTMUtils.comma_format(lg.get_dn_lanes()) + "}\n");
            writer.close();
            writer = null;
        } catch (IOException e) {
            return;
        }
    }

}
