package dispatch;

import common.AbstractSource;
import error.OTMException;
import models.SourceVehicle;

public class EventCreateVehicle extends AbstractEvent {

    public EventCreateVehicle(Dispatcher dispatcher, float timestamp, AbstractSource source) {
        super(dispatcher,4, timestamp,source);
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        SourceVehicle source = (SourceVehicle)recipient;
        source.insert_vehicle(timestamp);
        source.schedule_next_vehicle(dispatcher,timestamp);
    }

}
