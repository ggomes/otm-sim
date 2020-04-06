package actuator;

import dispatch.Dispatcher;
import error.OTMException;
import common.Scenario;

public class ActuatorVSL extends AbstractActuator {

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ActuatorVSL(Scenario scenario, jaxb.Actuator jaxb_actuator) throws OTMException {
        super(scenario,jaxb_actuator);
        System.err.println("ActuatorVSL is not implemented");
    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void process_controller_command(Object command, float timestamp) {

    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {

    }

    @Override
    public void register_with_dispatcher(Dispatcher dispatcher) {

    }
}
