package events;

import core.Scenario;
import error.OTMException;

public class EventLanegroupFD extends AbstractLanegroupEvent {

    public EventLanegroupFD(long id, EventType type, float timestamp) {
        super(id, type, timestamp);
    }

    public EventLanegroupFD(Scenario scenario, jaxb.Event jev) throws OTMException {
        super(scenario,jev);
    }
}
