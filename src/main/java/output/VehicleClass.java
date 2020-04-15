package output;

import api.info.events.EventVehicleInfo;
import commodity.Commodity;
import dispatch.Dispatcher;
import error.OTMException;
import models.vehicle.spatialq.MesoVehicle;
import models.vehicle.spatialq.Queue;
import runner.RunParameters;
import common.Scenario;

// TODO: THIS SHOULD NOT BE AN INTERFACEVEHICLELISTENER SINCE IT ONLY ACTS UPON VEHICLE CREATION
public class VehicleClass extends AbstractOutput implements InterfaceVehicleListener {

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public VehicleClass(Scenario scenario, String prefix, String output_folder) {
        super(scenario, prefix, output_folder);
        this.type = Type.vehicle_class;
    }

    //////////////////////////////////////////////////////
    // InterfaceOutput
    //////////////////////////////////////////////////////

    @Override
    public String get_output_file() {
        return write_to_file ? super.get_output_file() + "_vehicle_class.txt" : null;
    }

    @Override
    public void write(float timestamp, Object obj) throws OTMException {
        throw new OTMException("NOT IMPLEMENTED");
    }

    @Override
    public void register(RunParameters props, Dispatcher dispatcher) throws OTMException {
        // register with all commodities
        for (Commodity c : scenario.commodities.values())
            c.add_vehicle_event_listener(this);
    }

    //////////////////////////////////////////////////////
    // InterfaceVehicleListener
    //////////////////////////////////////////////////////

    @Override
    public void move_from_to_queue(float timestamp, MesoVehicle vehicle, Queue from_queue, Queue to_queue) throws OTMException {
        if(from_queue==null)
            write(timestamp,new EventVehicleInfo(vehicle.getId(),vehicle.get_commodity_id()));
    }

}
