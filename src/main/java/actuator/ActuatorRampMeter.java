package actuator;

import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import runner.Scenario;

public class ActuatorRampMeter extends AbstractActuator {

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ActuatorRampMeter(Scenario scenario, jaxb.Actuator jaxb_actuator) throws OTMException {
        super(scenario,jaxb_actuator);
        System.err.println("ActuatorRampMeter is not implemented");
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
