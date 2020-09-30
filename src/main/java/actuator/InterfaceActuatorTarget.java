package actuator;

import error.OTMException;

public interface InterfaceActuatorTarget {
//    long getIdAsTarget();
    void register_actuator(Long commid,AbstractActuator act) throws OTMException;
}
