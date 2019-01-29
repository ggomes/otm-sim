package models.micro;

import common.AbstractVehicle;
import output.InterfaceVehicleListener;

import java.util.Set;

public class Vehicle extends AbstractVehicle {

    public double pos;          // meters
    public double speed;        // meters per second

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public Vehicle(AbstractVehicle that){
        super(that);
        this.pos = 0d;
        this.speed = 0d;
    }

    public Vehicle(Long comm_id, Set<InterfaceVehicleListener> event_listeners){
        super(comm_id,event_listeners);
        this.pos = 0d;
        this.speed = 0d;
    }

}
