package actuator;

import error.OTMException;

public interface InterfaceActuatorTarget {
    void register_actuator(AbstractActuator act) throws OTMException;
}
