package api;

import error.OTMException;
import runner.Scenario;

public class OTMdev {

    public Scenario scenario;
    public OTM otm;

    public OTMdev(String config) throws OTMException {
        this.otm = new OTM(config,true,false);
        this.scenario = otm.scn;
    }

    public OTMdev(OTM otm){
        this.otm = otm;
        this.scenario = otm.scn;
    }

}
