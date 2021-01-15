package events;

import core.Scenario;
import error.OTMException;

public class EventLanegroupLanes extends AbstractLanegroupEvent {

    public EventLanegroupLanes(long id, EventType type, float timestamp) {
        super(id, type, timestamp);
    }

    public EventLanegroupLanes(Scenario scenario, jaxb.Event jev) throws OTMException {
        super(scenario,jev);
    }
}
