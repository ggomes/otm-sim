package events;

import core.AbstractLaneGroup;
import core.LaneGroupSet;
import core.Scenario;
import error.OTMException;
import utils.OTMUtils;

import java.util.Set;

public abstract class AbstractLanegroupEvent extends AbstractEvent {

    Set<AbstractLaneGroup> lanegroups;

    public AbstractLanegroupEvent(long id, EventType type, float timestamp) {
        super(id, type, timestamp);
    }

    public AbstractLanegroupEvent(Scenario scenario, jaxb.Event jev) throws OTMException {
        super(jev);

        if(jev.getEventTarget()!=null && jev.getEventTarget().getLanegroups()!=null){
            LaneGroupSet lgset = OTMUtils.read_lanegroups(jev.getEventTarget().getLanegroups(),scenario.network.links);
            this.lanegroups = lgset.lgs;
        }
    }
}
