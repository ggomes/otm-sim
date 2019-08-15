package api.info.events;

public abstract class AbstractEventInfo {

    public final float timestamp;

    public AbstractEventInfo(float timestamp) {
        this.timestamp = timestamp;
    }
}
