package api.info.events;

public class EventControllerScheduleTransitionInfo extends AbstractEventInfo {

    public final long controller_id;
    public final int current_item;

    public EventControllerScheduleTransitionInfo(float timestamp, long controller_id, int current_item) {
        super(timestamp);
        this.controller_id = controller_id;
        this.current_item = current_item;
    }

    @Override
    public String toString() {
        return new String(controller_id +"\t" + current_item);
    }
}
