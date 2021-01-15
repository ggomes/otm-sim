package events;

import core.Scenario;

public class EventLanegroupFD extends AbstractLanegroupEvent {

    public EventLanegroupFD(long id, EventType type, float timestamp) {
        super(id, type, timestamp);
    }

    public EventLanegroupFD(Scenario scenario, jaxb.Event jev){
        super(scenario,jev);
    }
}
