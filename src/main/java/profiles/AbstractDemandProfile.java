package profiles;

import commodity.Commodity;
import common.AbstractDemandGenerator;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import keys.DemandType;
import common.Scenario;

public abstract class AbstractDemandProfile {


    ////////////////////////////////////////////////////
    // abstract
    ////////////////////////////////////////////////////

//    abstract public void validate(OTMErrorLog errorLog);
//    abstract public void initialize(Scenario scenario) throws OTMException;
//    abstract public void register_with_dispatcher(Dispatcher dispatcher);
    abstract public DemandType get_type();
    abstract public Long get_origin_node_id();
    abstract public Long get_destination_node_id();

    ////////////////////////////////////////////////////
    // public
    ////////////////////////////////////////////////////



}
