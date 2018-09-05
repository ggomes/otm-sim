package models.pq;

import commodity.Commodity;
import commodity.Path;
import common.AbstractLaneGroup;
import common.Link;
import error.OTMException;
import common.AbstractVehicle;
import keys.KeyCommPathOrLink;
import output.InterfaceVehicleListener;

import java.util.Set;

public class Vehicle extends AbstractVehicle {

    public Queue my_queue;
    public boolean waiting_for_lane_change = true;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public Vehicle(){}

    public Vehicle(KeyCommPathOrLink key, Set<InterfaceVehicleListener> vehicle_event_listeners) {
        super(key,vehicle_event_listeners);
        this.waiting_for_lane_change = false;
    }

    public Vehicle(models.micro.Vehicle micro_vehicle) {
        super(micro_vehicle);
        this.waiting_for_lane_change = false;
    }

    ///////////////////////////////////////////////////
    // update
    ///////////////////////////////////////////////////

    /** used by EventTransitToWaiting **/
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

    /** used by create_vehicle and this.move_to_waiting_queue **/
    private void assign_queue(float timestamp,Queue to_queue) throws OTMException {

        throw new OTMException("NOT IMPLEMENTED");

//        // update vehicle queue reference
//        Queue from_queue = my_queue;
//        my_queue = to_queue;
//        in_lanegroup = my_lanegroup;
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
        my_lanegroup = to_queue.lanegroup;

        // inform listeners
        if(get_event_listeners()!=null)
            for(InterfaceVehicleListener ev : get_event_listeners())
                ev.move_from_to_queue(timestamp,this,from_queue,to_queue);
    }
}
