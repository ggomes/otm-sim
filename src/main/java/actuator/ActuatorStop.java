package actuator;

import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Actuator;
import common.Scenario;

public class ActuatorStop extends AbstractActuator {

    public ActuatorStop(Scenario scenario, Actuator jaxb_actuator) throws OTMException {
        super(scenario, jaxb_actuator);
    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void process_controller_command(Object command, float timestamp) throws OTMException {

    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {

    }

    @Override
    public void register_with_dispatcher(Dispatcher dispatcher) {

    }
}
