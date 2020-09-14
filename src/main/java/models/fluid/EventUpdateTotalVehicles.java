package models.fluid;

import dispatch.AbstractEvent;
import dispatch.Dispatcher;
import error.OTMException;
import output.OutputLaneGroupAvgVehicles;

public class EventUpdateTotalVehicles extends AbstractEvent {

    public EventUpdateTotalVehicles(Dispatcher dispatcher, float timestamp, Object recipient) {
        super(dispatcher, 69, timestamp, recipient);
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        OutputLaneGroupAvgVehicles obj = (OutputLaneGroupAvgVehicles)recipient;
        obj.update_total_vehicles(timestamp);
        dispatcher.register_event(new EventUpdateTotalVehicles(dispatcher,timestamp + obj.simDt,recipient));
    }
}
