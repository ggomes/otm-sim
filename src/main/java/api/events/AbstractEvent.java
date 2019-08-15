package api.events;

public abstract class AbstractEvent {

    public final float timestamp;

    public AbstractEvent(float timestamp) {
        this.timestamp = timestamp;
    }
}
