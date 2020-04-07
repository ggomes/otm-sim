package actuator;

import common.Link;
import common.RoadConnection;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Actuator;
import common.Scenario;

public class ActuatorCapacity extends AbstractActuator  {

    public float max_rate_vps;
    public float min_rate_vps;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ActuatorCapacity(Scenario scenario, Actuator jact) throws OTMException {
        super(scenario, jact);

        // interpret jact.getMaxValue and jact.getMinValue in vphpl
        int num_lanes = ((Link)target).full_lanes;
        max_rate_vps = jact.getMaxValue()>=0f ? jact.getMaxValue()*num_lanes/3600f : Float.POSITIVE_INFINITY;
        min_rate_vps = jact.getMinValue()>=0f ? jact.getMinValue()*num_lanes/3600f : 0f;
    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void initialize(Scenario scenario) throws OTMException {

    }

    ///////////////////////////////////////////////////
    // AbstractActuator
    ///////////////////////////////////////////////////

    @Override
    public void process_controller_command(Object command, float timestamp) throws OTMException {
        if(command==null)
            return;
        Link link = (Link) target;
        float rate_vps = (float) command;
        rate_vps = Math.max(Math.min(rate_vps,max_rate_vps),min_rate_vps);
        for(RoadConnection rc : link.outlink2roadconnection.values())
            rc.set_external_max_flow_vps(timestamp,rate_vps);
    }

}
