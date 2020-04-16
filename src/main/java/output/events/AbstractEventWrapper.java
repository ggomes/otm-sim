package output.events;

public abstract class AbstractEventWrapper {

    public final float timestamp;
    abstract public String asString();

    public AbstractEventWrapper(float timestamp) {
        this.timestamp = timestamp;
    }
}
