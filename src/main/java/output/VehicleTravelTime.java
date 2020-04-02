package output;

import api.info.events.EventVehicleTravelTimeInfo;
import commodity.Commodity;
import dispatch.Dispatcher;
import error.OTMException;
import models.vehicle.spatialq.MesoVehicle;
import models.vehicle.spatialq.Queue;
import runner.RunParameters;
import runner.Scenario;

public class VehicleTravelTime extends AbstractOutputEvent implements InterfaceVehicleListener  {

    public VehicleTravelTime(Scenario scenario, String prefix, String output_folder) {
        super(scenario, prefix, output_folder);
        this.type = Type.vehicle_travel_time;
    }

    @Override
    public String get_output_file() {
        return write_to_file ? super.get_output_file() + "_vehicle_travel_time.txt" : null;
    }

    @Override
    public void register(RunParameters props, Dispatcher dispatcher) throws OTMException {
        // register with all commodities
        for (Commodity c : scenario.commodities.values())
            c.add_vehicle_event_listener(this);
    }

    @Override
    public void move_from_to_queue(float timestamp, MesoVehicle vehicle, Queue from_queue, Queue to_queue) throws OTMException {
        write(timestamp,new EventVehicleTravelTimeInfo(timestamp,vehicle.getId(),from_queue,to_queue));
    }

    @Override
    public void plot(String filename) throws OTMException {
        System.err.println("IMPLEMENT THIS");
    }

}
