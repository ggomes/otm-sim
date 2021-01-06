package core;

import actuator.AbstractActuator;
import actuator.InterfaceTarget;
import error.OTMException;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class LaneGroupSet implements InterfaceTarget {

    public Set<AbstractLaneGroup> lgs = new HashSet<>();

    @Override
    public String getTypeAsTarget() {
        return "lanegroups";
    }

    @Override
    public long getIdAsTarget() {
        return 0;
    }

    @Override
    public AbstractModel get_model() {
        Set<AbstractModel> models = lgs.stream().map(lg->lg.get_link().get_model()).collect(Collectors.toSet());
        return models.size()==1 ? models.iterator().next() : null;
    }

    @Override
    public void register_actuator(Set<Long> commids,AbstractActuator act,boolean override) throws OTMException {
        for(AbstractLaneGroup lg : lgs)
            lg.register_actuator(commids,act,override);
    }
}
