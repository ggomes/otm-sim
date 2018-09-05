package models.micro;

import commodity.Commodity;
import commodity.Path;
import common.AbstractVehicle;
import keys.KeyCommPathOrLink;
import output.InterfaceVehicleListener;

import java.util.Set;

public class Vehicle extends AbstractVehicle {

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public Vehicle(){}

    public Vehicle(KeyCommPathOrLink key, Set<InterfaceVehicleListener> vehicle_event_listeners) {
        super(key,vehicle_event_listeners);
    }

    public Vehicle(models.pq.Vehicle meso_vehicle) {
        super(meso_vehicle);
    }

    public void move_to_lanegroup(float timestamp){
        System.out.println(timestamp + "\tmove to lanegroup " + this.getId());
    }

}
