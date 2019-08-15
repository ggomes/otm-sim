package api;

import error.OTMException;
import output.LaneGroups;
import runner.Scenario;

public class APIOutput {

    private Scenario scenario;
    protected APIOutput(Scenario scenario){
        this.scenario = scenario;
    }

    public void request_lanegroups(String prefix,String output_folder){
        try {
            this.scenario.outputs.add(new LaneGroups(scenario,prefix,output_folder));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

}
