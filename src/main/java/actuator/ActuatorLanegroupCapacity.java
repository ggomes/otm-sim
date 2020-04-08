package actuator;

import common.Scenario;
import error.OTMException;
import jaxb.Actuator;
import common.AbstractLaneGroup;

public class ActuatorLanegroupCapacity extends AbstractActuator {

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ActuatorLanegroupCapacity(Scenario scenario, Actuator jaxb_actuator) throws OTMException {
        super(scenario, jaxb_actuator);
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
        AbstractLaneGroup lg = (AbstractLaneGroup) target;
        float rate_vps = (float) command;
        lg.set_actuator_capacity_vps(rate_vps);
    }

}
