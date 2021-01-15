package events;

import core.Scenario;

public abstract class AbstractEvent {

    public enum EventType {linktgl,cntrltgl,lglanes,lgfd};

    public final long id;
    public final EventType type;
    public float timestamp;

    public AbstractEvent(long id, EventType type, float timestamp) {
        this.id = id;
        this.type = type;
        this.timestamp = timestamp;
    }

    public AbstractEvent(jaxb.Event jev){
        this(jev.getId(),EventType.valueOf(jev.getType()),jev.getTimestamp());
    }

}
