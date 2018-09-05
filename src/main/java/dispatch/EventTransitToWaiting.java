package dispatch;

import error.OTMException;
import models.pq.LaneGroup;
import models.pq.Vehicle;

public class EventTransitToWaiting extends AbstractEvent {

    public EventTransitToWaiting(Dispatcher dispatcher,float timestamp, Object vehicle) {
        super(dispatcher,0,timestamp,vehicle);
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        Vehicle vehicle = (Vehicle)recipient;
        LaneGroup lanegroup = (LaneGroup) vehicle.get_lanegroup();
        if(verbose)
            System.out.println(String.format("\tvehicle %d, link %d, lanegroup %d",vehicle.getId(),lanegroup.link.getId(),lanegroup.id));

        vehicle.move_to_queue(timestamp,lanegroup.waiting_queue);
    }

}
