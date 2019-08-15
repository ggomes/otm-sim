package api;

import runner.Scenario;

public class APIdev {

    public Scenario scenario;
    public API api;

    public APIdev(API api){
        this.api = api;
        this.scenario = api.scn;
    }

}
