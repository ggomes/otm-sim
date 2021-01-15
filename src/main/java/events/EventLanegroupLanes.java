package events;

import core.Scenario;

public class EventLanegroupLanes extends AbstractLanegroupEvent {

    public EventLanegroupLanes(long id, EventType type, float timestamp) {
        super(id, type, timestamp);
    }

    public EventLanegroupLanes(Scenario scenario, jaxb.Event jev){
        super(scenario,jev);
    }
}
