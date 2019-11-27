package output;

import api.info.events.EventVehicleFromToQueueInfo;
import commodity.Commodity;
import dispatch.Dispatcher;
import error.OTMException;
import models.spatialq.Queue;
import models.spatialq.Vehicle;
import runner.RunParameters;
import runner.Scenario;

public class EventsVehicle extends AbstractOutputEvent implements InterfaceVehicleListener {

    private final String suffix;
    public Long commodity_id;

    public EventsVehicle(Scenario scenario, String prefix, String output_folder, Long commodity_id) throws OTMException {
        super(scenario,prefix,output_folder);
        this.type = Type.vehicle_events;

        this.suffix = commodity_id==null ? "all" : commodity_id.toString();
        this.commodity_id = commodity_id;
    }

    @Override
    public void register(RunParameters props, Dispatcher dispatcher) throws OTMException {
        // register with the commodity
        Commodity commodity = scenario.commodities.get(commodity_id);
        if(commodity!=null)
            commodity.add_vehicle_event_listener(this);
        else   // register with all commodities
            for (Commodity c : scenario.commodities.values())
                c.add_vehicle_event_listener(this);
    }

    @Override
    public String get_output_file() {
        return write_to_file ? super.get_output_file() + "_vehicle_events_" + suffix + ".txt" : null;
    }


    @Override
    public void plot(String filename) throws OTMException {
        System.err.println("IMPLEMENT THIS");
    }

    //////////////////////////////////////////////////////
    // write
    //////////////////////////////////////////////////////

    public void move_from_to_queue(float timestamp, Vehicle vehicle, Queue from_queue, Queue to_queue) throws OTMException {
        this.write(timestamp,new EventVehicleFromToQueueInfo(timestamp,vehicle.getId(),from_queue,to_queue));
    }

}
