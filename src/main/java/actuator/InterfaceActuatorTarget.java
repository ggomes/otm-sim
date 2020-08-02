package actuator;

import error.OTMException;

public interface InterfaceActuatorTarget {
//    long getIdAsTarget();
    void register_actuator(AbstractActuator act) throws OTMException;
}
