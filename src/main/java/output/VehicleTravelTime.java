/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package output;

import api.events.EventVehicleTravelTime;
import commodity.Commodity;
import dispatch.Dispatcher;
import error.OTMException;
import models.pq.Queue;
import models.pq.Vehicle;
import runner.RunParameters;
import runner.Scenario;

public class VehicleTravelTime extends AbstractOutputEvent implements InterfaceVehicleListener  {

    public VehicleTravelTime(Scenario scenario, String prefix, String output_folder) {
        super(scenario, prefix, output_folder);
        this.type = Type.vehicle_travel_time;
    }

    @Override
    public String get_output_file() {
        return super.get_output_file() + "_vehicle_travel_time.txt";
    }

    @Override
    public void register(RunParameters props, Dispatcher dispatcher) throws OTMException {
        // register with all commodities
        for (Commodity c : scenario.commodities.values())
            c.add_vehicle_event_listener(this);
    }

    @Override
    public void move_from_to_queue(float timestamp, Vehicle vehicle, Queue from_queue, Queue to_queue) throws OTMException {
        write(timestamp,new EventVehicleTravelTime(timestamp,vehicle.getId(),from_queue,to_queue));
    }

    @Override
    public void plot(String filename) throws OTMException {
        System.out.println("IMPLEMENT THIS");
    }

}
