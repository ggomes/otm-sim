package output;

import common.AbstractLaneGroup;
import common.Link;
import dispatch.Dispatcher;
import error.OTMException;
import runner.RunParameters;
import common.Scenario;
import utils.OTMUtils;

import java.io.IOException;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class OutputLaneGroups extends AbstractOutput {

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public OutputLaneGroups(Scenario scenario, String prefix, String output_folder) throws OTMException {
        super(scenario,prefix,output_folder);
    }

    //////////////////////////////////////////////////////
    // InterfaceOutput
    //////////////////////////////////////////////////////

    @Override
    public String get_output_file() {
        return write_to_file ? super.get_output_file() + "_lanegroups.txt" : null;
    }

    public void write(float timestamp, Object obj) {
        System.err.println("this should not happen");
    }

    @Override
    public void register(RunParameters props, Dispatcher dispatcher) {
        if(writer==null)
            return;
        try {

            for(Link link : scenario.network.links.values())
                for(AbstractLaneGroup lg : link.lanegroups_flwdn)
                    writer.write(dnlgstring(lg));

            writer.close();
            writer = null;
        } catch (IOException e) {
            return;
        }
    }

    private static String dnlgstring(AbstractLaneGroup lg){
        return String.format("%d\t%d\t%d\t%d\t%d\n",lg.id , lg.link.getId(),0,lg.start_lane_dn,lg.num_lanes);
    }

    private static String uplgstring(AbstractLaneGroup lg){
        return String.format("%d\t%d\t%d\t%d\t%d\n",lg.id , lg.link.getId(),1,lg.start_lane_up,lg.num_lanes);
    }

}
