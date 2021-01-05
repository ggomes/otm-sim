package models.vehicle;

import core.AbstractVehicle;
import models.InterfaceModel;
import output.InterfaceVehicleListener;

import java.util.Set;

public interface InterfaceVehicleModel extends InterfaceModel {
    AbstractVehicle translate_vehicle(AbstractVehicle that);
    AbstractVehicle create_vehicle(Long comm_id, Set<InterfaceVehicleListener> event_listeners);
}
