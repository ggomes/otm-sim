package models.vehicle.newell;

import core.AbstractVehicle;
import output.InterfaceVehicleListener;

import java.util.Set;

public class NewellVehicle extends AbstractVehicle {

    public double pos;          // meters
    public double headway;      // meters
    public double new_pos;      // meters

    public NewellVehicle leader;
    public NewellVehicle follower;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public NewellVehicle(AbstractVehicle that){
        super(that);
        this.pos = 0d;
        this.new_pos = 0d;
        this.headway = Double.NaN;
        this.leader = null;
        this.follower = null;
    }

    public NewellVehicle(Long comm_id, Set<InterfaceVehicleListener> event_listeners){
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
