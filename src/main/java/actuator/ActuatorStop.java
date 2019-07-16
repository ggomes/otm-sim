package actuator;

import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Actuator;
import runner.Scenario;

public class ActuatorStop extends AbstractActuator {

    public ActuatorStop(Scenario scenario, Actuator jaxb_actuator) throws OTMException {
        super(scenario, jaxb_actuator);
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {

    }

    @Override
    public void process_controller_command(Object command, Dispatcher dispatcher, float timestamp) throws OTMException {

    }
}
