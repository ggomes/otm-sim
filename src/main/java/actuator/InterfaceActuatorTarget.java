package actuator;

import error.OTMException;

import java.util.Set;

public interface InterfaceActuatorTarget {

    String getTypeAsTarget();
    long getIdAsTarget();
    void register_actuator(Set<Long> commids, AbstractActuator act) throws OTMException;
}
