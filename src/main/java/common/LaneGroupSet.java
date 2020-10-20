package common;

import actuator.AbstractActuator;
import actuator.InterfaceActuatorTarget;
import error.OTMException;

import java.util.HashSet;
import java.util.Set;

public class LaneGroupSet implements InterfaceActuatorTarget {

    public Set<AbstractLaneGroup> lgs = new HashSet<>();

    @Override
    public long getIdAsTarget() {
        return 0;
    }

    @Override
    public void register_actuator(Set<Long> commids,AbstractActuator act) throws OTMException {
        for(AbstractLaneGroup lg : lgs)
            lg.register_actuator(commids,act);
    }
}
