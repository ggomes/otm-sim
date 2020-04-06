package actuator;

import error.OTMException;
import common.InterfaceScenarioElement;

public interface InterfaceActuatorTarget extends InterfaceScenarioElement {
    void register_actuator(AbstractActuator act) throws OTMException;
}
