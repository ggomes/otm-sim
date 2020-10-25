package models.fluid;

import dispatch.AbstractEvent;
import dispatch.Dispatcher;
import error.OTMException;
import output.OutputCellSumVehiclesDwn;

public class EventUpdateTotalCellVehiclesDwn extends AbstractEvent {

    public EventUpdateTotalCellVehiclesDwn(Dispatcher dispatcher, float timestamp, Object recipient) {
        super(dispatcher, 69, timestamp, recipient);
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        OutputCellSumVehiclesDwn obj = (OutputCellSumVehiclesDwn)recipient;
        obj.update_total_vehicles(timestamp);
        dispatcher.register_event(new EventUpdateTotalCellVehiclesDwn(dispatcher,timestamp + obj.simDt,recipient));
    }

}
