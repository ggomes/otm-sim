package models.fluid;

import dispatch.AbstractEvent;
import dispatch.Dispatcher;
import error.OTMException;
import output.OutputLaneGroupSumVehicles;

public class EventUpdateTotalLanegroupVehicles extends AbstractEvent {

    public EventUpdateTotalLanegroupVehicles(Dispatcher dispatcher, float timestamp, Object recipient) {
        super(dispatcher, 69, timestamp, recipient);
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        OutputLaneGroupSumVehicles obj = (OutputLaneGroupSumVehicles)recipient;
        obj.update_total_vehicles(timestamp);
        dispatcher.register_event(new EventUpdateTotalLanegroupVehicles(dispatcher,timestamp + obj.simDt,recipient));
    }
}
