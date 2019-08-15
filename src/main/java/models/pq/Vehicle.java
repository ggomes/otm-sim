package models.pq;

import error.OTMException;
import common.AbstractVehicle;
import output.InterfaceVehicleListener;

import java.util.Set;

public class Vehicle extends AbstractVehicle {

    public Queue my_queue;
    public boolean waiting_for_lane_change;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public Vehicle(AbstractVehicle that){
        super(that);
        this.waiting_for_lane_change = false;
    }

    public Vehicle(Long comm_id, Set<InterfaceVehicleListener> event_listeners){
        super(comm_id,event_listeners);
        this.waiting_for_lane_change = false;
    }

    ///////////////////////////////////////////////////
    // update
    ///////////////////////////////////////////////////

    // used by EventTransitToWaiting
    public void move_to_waiting_queue(float timestamp) throws OTMException {

        Queue from_queue = my_queue;

        if(from_queue.type!= Queue.Type.transit)
            throw new OTMException("Not in a transit queue");

        // remove vehicle from the transit queue
        from_queue.remove_given_vehicle(timestamp,this);

        // add to waiting queue
        Queue to_queue = from_queue.lanegroup.waiting_queue;
        to_queue.add_vehicle(this);

        // tell the vehicle
        assign_queue(timestamp,to_queue);
    }

    // used by create_vehicle and this.move_to_waiting_queue
    private void assign_queue(float timestamp,Queue to_queue) throws OTMException {

        throw new OTMException("NOT IMPLEMENTED");

//        // update vehicle queue reference
//        Queue from_queue = my_queue;
//        my_queue = to_queue;
//        in_lanegroup = lg;
//
//        // inform listener
//        if(get_event_listener()!=null)
//            get_event_listener().move_from_to_queue(timestamp,this,from_queue,queue);
    }

//    public void move_to_lanegroup(float timestamp, AbstractLaneGroup lanegroup) throws OTMException {
//        if(lanegroup.link.model_type==Link.ModelType.models.ctm.pq)
//            assign_queue(timestamp,((models.ctm.pq.LaneGroup) lanegroup).transit_queue);
//    }

    ////////////////////////////////////////////////
    // move the vehicle
    ////////////////////////////////////////////////

    public void move_to_queue(float timestamp, Queue to_queue) throws OTMException {

        Queue from_queue = my_queue;

        // remove vehicle from its current queue
        if(from_queue!=null)
            from_queue.remove_given_vehicle(timestamp,this);

        // add to the to_queue
        to_queue.add_vehicle(this);

        // update vehicle queue reference
        my_queue = to_queue;
        lg = to_queue.lanegroup;

    }
}
