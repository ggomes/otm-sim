package models.fluid;

import dispatch.AbstractEvent;
import dispatch.Dispatcher;
import error.OTMException;
import output.OutputLinkSumVehicles;

public class EventUpdateTotalLinkVehicles extends AbstractEvent {

    public EventUpdateTotalLinkVehicles(Dispatcher dispatcher, float timestamp, Object recipient) {
        super(dispatcher, 69, timestamp, recipient);
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        OutputLinkSumVehicles obj = (OutputLinkSumVehicles)recipient;
        obj.update_total_vehicles(timestamp);
        dispatcher.register_event(new EventUpdateTotalLinkVehicles(dispatcher,timestamp + obj.simDt,recipient));
    }
}
