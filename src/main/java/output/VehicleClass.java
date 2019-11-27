package output;

import api.info.events.EventVehicleInfo;
import commodity.Commodity;
import dispatch.Dispatcher;
import error.OTMException;
import models.spatialq.Queue;
import models.spatialq.Vehicle;
import runner.RunParameters;
import runner.Scenario;

public class VehicleClass extends AbstractOutputEvent implements InterfaceVehicleListener {

    public VehicleClass(Scenario scenario, String prefix, String output_folder) {
        super(scenario, prefix, output_folder);
        this.type = Type.vehicle_class;
    }


    @Override
    public String get_output_file() {
        return write_to_file ? super.get_output_file() + "_vehicle_class.txt" : null;
    }

    @Override
    public void register(RunParameters props, Dispatcher dispatcher) throws OTMException {
        // register with all commodities
        for (Commodity c : scenario.commodities.values())
            c.add_vehicle_event_listener(this);
    }

    @Override
    public void move_from_to_queue(float timestamp, Vehicle vehicle, Queue from_queue, Queue to_queue) throws OTMException {
        if(from_queue==null)
            write(timestamp,new EventVehicleInfo(vehicle.getId(),vehicle.get_commodity_id()));
    }

    @Override
    public void plot(String filename) throws OTMException {
        System.err.println("IMPLEMENT THIS");
    }

}
