package api;

import runner.Scenario;

public class APIopen {

    public API api;

    public APIopen(API api){
        this.api = api;
    }

    public Scenario scenario(){
        return api.scenario;
    }

}
