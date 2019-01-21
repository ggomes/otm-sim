package models.micro;

import common.AbstractVehicle;
import keys.KeyCommPathOrLink;
import models.AbstractLaneGroup;
import output.InterfaceVehicleListener;

import java.util.Set;

public class Vehicle extends AbstractVehicle {

    public AbstractLaneGroup lg;
    public double position;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public Vehicle(){}

    public Vehicle(KeyCommPathOrLink key, Set<InterfaceVehicleListener> vehicle_event_listeners,AbstractLaneGroup lg) {
        super(key,vehicle_event_listeners);
        this.lg = lg;
        this.position = 0d;
    }

    ///////////////////////////////////////////////////
    // update
    ///////////////////////////////////////////////////

}
