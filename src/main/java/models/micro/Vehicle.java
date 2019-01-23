package models.micro;

import commodity.Commodity;
import common.AbstractVehicle;

public class Vehicle extends AbstractVehicle {

    public double pos;          // meters
    public double speed;        // meters per second

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public Vehicle(Commodity comm){
        super(comm);
        this.pos = 0d;
        this.speed = 0d;
    }

}
