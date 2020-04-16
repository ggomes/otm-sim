package output.events;

import models.vehicle.spatialq.Queue;

public class EventVehicleFromToQueueInfo extends AbstractEventWrapper {

    public final long vehicle_id;
    public final Queue from_queue;
    public final Queue to_queue;

    ///////////////////////////////////////
    // construction
    ///////////////////////////////////////

    public EventVehicleFromToQueueInfo(float timestamp, long vehicle_id, Queue from_queue, Queue to_queue) {
        super(timestamp);
        this.vehicle_id = vehicle_id;
        this.from_queue = from_queue;
        this.to_queue = to_queue;
    }

    ///////////////////////////////////////
    // toString
    ///////////////////////////////////////

    @Override
    public String asString() {
        return get_vehicle_id()+"\t"+from_queue_id()+"\t"+to_queue_id();
    }

    ///////////////////////////////////////
    // get
    ///////////////////////////////////////

    public Long get_vehicle_id(){
        return vehicle_id;
    }

    public String from_queue_id(){
        return from_queue==null? "-" : from_queue.id;
    }

    public String to_queue_id(){
        return to_queue==null? "-" : to_queue.id;
    }


}
