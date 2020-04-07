package actuator;

import error.OTMException;
import common.InterfaceScenarioElement;

public interface InterfaceActuatorTarget {
    void register_actuator(AbstractActuator act) throws OTMException;
}
