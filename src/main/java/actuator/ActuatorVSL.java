package actuator;

import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import runner.Scenario;

public class ActuatorVSL extends AbstractActuator {

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ActuatorVSL(Scenario scenario, jaxb.Actuator jaxb_actuator) throws OTMException {
        super(scenario,jaxb_actuator);
        System.err.println("ActuatorVSL is not implemented");
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
    }

    @Override
    public void process_controller_command(Object command, Dispatcher dispatcher, float timestamp) {

    }
}
