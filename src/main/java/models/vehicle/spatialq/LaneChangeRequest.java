package models.vehicle.spatialq;

import error.OTMException;

public class LaneChangeRequest {

    protected float timestamp;
    protected Vehicle requester;
    protected Queue from_queue;
    protected Queue to_queue;

    public LaneChangeRequest(float timestamp, Vehicle requester, Queue from_queue, Queue to_queue) throws OTMException {
        if(from_queue.lanegroup.link!=to_queue.lanegroup.link)
            throw new OTMException("Lane change must happen within a link");
        this.timestamp = timestamp;
        this.requester = requester;
        this.from_queue = from_queue;
        this.to_queue = to_queue;
    }

    public static int compareTimestamp(LaneChangeRequest e1, LaneChangeRequest e2){
        return e1.timestamp>e2.timestamp ? 1 : -1;
    }


}
