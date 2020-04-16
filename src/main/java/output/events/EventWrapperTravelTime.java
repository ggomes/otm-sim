package output.events;

import models.vehicle.spatialq.Queue;

public class EventWrapperTravelTime extends AbstractEventWrapper {

    public final long vehicle_id;
    public final Queue from_queue;
    public final Queue to_queue;

    public EventWrapperTravelTime(float timestamp, long vehicle_id, Queue from_queue, Queue to_queue) {
        super(timestamp);
        this.vehicle_id = vehicle_id;
        this.from_queue = from_queue;
        this.to_queue = to_queue;
    }

    @Override
    public String asString() {
        if(from_queue==null)
            return vehicle_id + "\t" + "1";
        if(to_queue==null)
            return vehicle_id + "\t" + "0";
        return "";
    }
}
