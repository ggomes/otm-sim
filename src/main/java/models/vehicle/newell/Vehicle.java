package models.vehicle.newell;

import common.AbstractVehicle;
import output.InterfaceVehicleListener;

import java.util.Set;

public class Vehicle extends AbstractVehicle {

    public double pos;          // meters
    public double headway;      // meters
    public double new_pos;      // meters

    public Vehicle leader;
    public Vehicle follower;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public Vehicle(AbstractVehicle that){
        super(that);
        this.pos = 0d;
        this.new_pos = 0d;
        this.headway = Double.NaN;
        this.leader = null;
        this.follower = null;
    }

    public Vehicle(Long comm_id, Set<InterfaceVehicleListener> event_listeners){
        super(comm_id,event_listeners);
        this.pos = 0d;
        this.new_pos = 0d;
        this.headway = Double.NaN;
        this.leader = null;
        this.follower = null;
    }

//    public static double initialize_headway(Vehicle leader,Vehicle follower){
//
//        if(leader==null)
//            return Double.POSITIVE_INFINITY;
//
//        if(leader.get_lanegroup()==follower.get_lanegroup())
//            return leader.pos - follower.pos;
//
//        else
//            return follower.get_lanegroup().length + leader.pos - follower.pos;
//
//    }


}
