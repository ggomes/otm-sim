package dispatch;

import common.AbstractDemandGenerator;
import error.OTMException;
import models.vehicle.VehicleDemandGenerator;

public class EventCreateVehicle extends AbstractEvent {

    public EventCreateVehicle(Dispatcher dispatcher, float timestamp, AbstractDemandGenerator source) {
        super(dispatcher,40, timestamp,source);
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        VehicleDemandGenerator source = (VehicleDemandGenerator)recipient;
        source.insert_vehicle(timestamp);
        source.schedule_next_vehicle(dispatcher,timestamp);
    }

}
