package models.ctm;

import error.OTMErrorLog;
import error.OTMException;
import runner.Scenario;

public interface InterfaceNodeModel {

    void validate(OTMErrorLog errorLog);

    void initialize(Scenario scenario) throws OTMException ;

//    void exchange_packets(float timestamp) throws OTMException;

    void update_flow(float timestamp,boolean is_sink);

}
