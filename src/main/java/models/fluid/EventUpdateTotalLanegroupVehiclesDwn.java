package models.fluid;

import dispatch.AbstractEvent;
import dispatch.Dispatcher;
import error.OTMException;
import output.OutputLaneGroupSumVehiclesDwn;

public class EventUpdateTotalLanegroupVehiclesDwn extends AbstractEvent {

    public EventUpdateTotalLanegroupVehiclesDwn(Dispatcher dispatcher, float timestamp, Object recipient) {
        super(dispatcher, 69, timestamp, recipient);
    }

    @Override
    public void action() throws OTMException {
        OutputLaneGroupSumVehiclesDwn obj = (OutputLaneGroupSumVehiclesDwn)recipient;
        obj.update_total_vehicles(timestamp);
        dispatcher.register_event(new EventUpdateTotalLanegroupVehiclesDwn(dispatcher,timestamp + obj.simDt,recipient));
    }
}
