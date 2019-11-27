package output;

import error.OTMException;
import models.spatialq.Queue;
import models.spatialq.Vehicle;

public interface InterfaceVehicleListener {

    void move_from_to_queue(float timestamp, Vehicle vehicle, Queue from_queue, Queue to_queue) throws OTMException;

}

