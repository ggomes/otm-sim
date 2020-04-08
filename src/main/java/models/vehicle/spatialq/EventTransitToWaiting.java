package models.vehicle.spatialq;

import dispatch.AbstractEvent;
import dispatch.Dispatcher;
import error.OTMException;
import models.vehicle.spatialq.MesoLaneGroup;
import models.vehicle.spatialq.MesoVehicle;
import output.InterfaceVehicleListener;

public class EventTransitToWaiting extends AbstractEvent {

    public EventTransitToWaiting(Dispatcher dispatcher, float timestamp, Object vehicle) {
        super(dispatcher,4,timestamp,vehicle);
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        MesoVehicle vehicle = (MesoVehicle)recipient;
        MesoLaneGroup lanegroup = (MesoLaneGroup) vehicle.get_lanegroup();
        if(verbose)
            System.out.println(String.format("\tvehicle %d, link %d, lanegroup %d",vehicle.getId(),lanegroup.link.getId(),lanegroup.id));

        // inform listeners
        if(vehicle.get_event_listeners()!=null)
            for(InterfaceVehicleListener ev : vehicle.get_event_listeners())
                ev.move_from_to_queue(timestamp,vehicle,vehicle.my_queue,lanegroup.waiting_queue);

        vehicle.move_to_queue(timestamp,lanegroup.waiting_queue);

    }

}
