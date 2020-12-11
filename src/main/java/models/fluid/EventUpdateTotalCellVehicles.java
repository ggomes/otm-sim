package models.fluid;

import dispatch.AbstractEvent;
import dispatch.Dispatcher;
import error.OTMException;
import output.OutputCellSumVehicles;

public class EventUpdateTotalCellVehicles extends AbstractEvent {

    public EventUpdateTotalCellVehicles(Dispatcher dispatcher, float timestamp, Object recipient) {
        super(dispatcher, 69, timestamp, recipient);
    }

    @Override
    public void action() throws OTMException {
        OutputCellSumVehicles obj = (OutputCellSumVehicles)recipient;
        obj.update_total_vehicles(timestamp);
        dispatcher.register_event(new EventUpdateTotalCellVehicles(dispatcher,timestamp + obj.simDt,recipient));
    }

}
