package output;

import error.OTMException;
import models.vehicle.spatialq.Queue;
import models.vehicle.spatialq.MesoVehicle;

public interface InterfaceVehicleListener {

    void move_from_to_queue(float timestamp, MesoVehicle vehicle, Queue from_queue, Queue to_queue) throws OTMException;

}

