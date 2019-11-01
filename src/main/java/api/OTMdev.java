package api;

import error.OTMException;
import runner.Scenario;

/**
 * Superuser API. It provides access to standard API methods and also exposes the internal Scenario class.
 * This class should be used for development and debugging purposes only.
 */
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
