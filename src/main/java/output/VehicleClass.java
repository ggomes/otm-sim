/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package output;

import api.events.EventVehicleClass;
import commodity.Commodity;
import dispatch.Dispatcher;
import error.OTMException;
import models.pq.Queue;
import models.pq.Vehicle;
import runner.RunParameters;
import runner.Scenario;

public class VehicleClass extends AbstractOutputEvent implements InterfaceVehicleListener {

    public VehicleClass(Scenario scenario, String prefix, String output_folder) {
        super(scenario, prefix, output_folder);
        this.type = Type.vehicle_class;
    }


    @Override
    public String get_output_file() {
        return super.get_output_file() + "_vehicle_class.txt";
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
            write(timestamp,new EventVehicleClass(vehicle.getId(),vehicle.get_commodity_id()));
    }

    @Override
    public void plot(String filename) throws OTMException {
        System.err.println("IMPLEMENT THIS");
    }

}
