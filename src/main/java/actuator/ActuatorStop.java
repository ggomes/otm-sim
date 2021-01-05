package actuator;

import core.Scenario;
import error.OTMException;
import jaxb.Actuator;

public class ActuatorStop extends AbstractActuatorLanegroupCapacity  {

    public ActuatorStop(Scenario scenario, Actuator jact) throws OTMException {
        super(scenario, jact);
    }

    @Override
    public Type getType() {
        return Type.stop;
    }

}
