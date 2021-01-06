package actuator;

import core.AbstractModel;
import error.OTMException;

import java.util.Set;

public interface InterfaceTarget {
    String getTypeAsTarget();
    long getIdAsTarget();
    AbstractModel get_model();
    void register_actuator(Set<Long> commids, AbstractActuator act,boolean override) throws OTMException;
}
