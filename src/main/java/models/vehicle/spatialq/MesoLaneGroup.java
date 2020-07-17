package models.vehicle.spatialq;

import common.*;
import error.OTMErrorLog;
import error.OTMException;
import dispatch.Dispatcher;
import geometry.FlowPosition;
import geometry.Side;
import keys.State;
import common.AbstractLaneGroup;
import models.vehicle.VehicleLaneGroup;
import output.InterfaceVehicleListener;
import packet.PacketLaneGroup;
import packet.PacketLink;
import common.Scenario;
import traveltime.VehicleLaneGroupTimer;
import utils.OTMUtils;

import java.util.*;

public class MesoLaneGroup extends VehicleLaneGroup {

    public Queue transit_queue;
    public Queue waiting_queue;

    // nominal parameters
    public float nom_transit_time_sec;
    public float nom_saturation_flow_rate_vps;

    // applied (actuated) fd
    public float transit_time_sec;
    public double saturation_flow_rate_vps;

    ////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public MesoLaneGroup(Link link, Side side, FlowPosition flwpos, float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs){
        super(link, side,flwpos,length, num_lanes, start_lane, out_rcs);
        this.transit_queue = new Queue(this, Queue.Type.transit);
        this.waiting_queue = new Queue(this, Queue.Type.waiting);
    }

    ////////////////////////////////////////////
    // InterfaceScenarioElement-like
    ///////////////////////////////////////////

    @Override
    public void validate(OTMErrorLog errorLog) {
        transit_queue.validate(errorLog);
        waiting_queue.validate(errorLog);
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
        transit_queue.initialize();
        waiting_queue.initialize();
//        current_max_flow_rate_vps = saturation_flow_rate_vps;

        // register first vehicle exit
        float start_time = scenario.get_current_time();
        schedule_release_vehicle(start_time);

        update_supply();
    }

    ////////////////////////////////////////////
    // AbstractLaneGroup
    ///////////////////////////////////////////

    @Override
    public void set_road_params(jaxb.Roadparam r){
        super.set_road_params(r);

        nom_transit_time_sec = (length/r.getSpeed())* 3.6f; // [m]/[kph] -> [sec]
        nom_saturation_flow_rate_vps = r.getCapacity()*num_lanes/3600f;

        transit_time_sec = nom_transit_time_sec;
        saturation_flow_rate_vps = nom_saturation_flow_rate_vps;
    }

    ////////////////////////////////////////////
    // InterfaceLaneGroup
    ///////////////////////////////////////////

    @Override
    public void set_actuator_capacity_vps(double rate_vps) {
        if(rate_vps<-OTMUtils.epsilon)
            return;
        this.saturation_flow_rate_vps = Math.min(nom_saturation_flow_rate_vps,rate_vps);
    }

    @Override
    public void set_actuator_speed_mps(double speed_mps) {
        if(speed_mps<OTMUtils.epsilon)
            return;
        this.transit_time_sec = Math.max(nom_transit_time_sec,length/((float) speed_mps));
    }

    @Override
    public void allocate_state() {
    }

    @Override
    public void update_supply() {
        supply =  max_vehicles - get_total_vehicles();
    }

    @Override
    public Double get_upstream_vehicle_position(){
        return supply * length / max_vehicles;
    }

    /**
     * A packet arrives at this lanegroup.
     * Vehicles do not know their next_link. It is assumed that the packet fits in this lanegroup.
     * 1. convert the packet to models.fluid.ctm.micro, models.fluid.ctm.pq, or models.fluid.ctm. This involves memory kept in the lanegroup.
     * 2. tag it with next_link and target lanegroups.
     * 3. add the packet to this lanegroup.
     */
    @Override
    public void add_vehicle_packet(float timestamp, PacketLaneGroup vp, Long next_link_id) throws OTMException {

        // for each vehicle
        Dispatcher dispatcher = link.network.scenario.dispatcher;
        for(AbstractVehicle absveh : create_vehicles_from_packet(vp,next_link_id)){

            MesoVehicle veh = (MesoVehicle) absveh;

            // tell the event listeners
            if(veh.get_event_listeners()!=null)
                for(InterfaceVehicleListener ev : veh.get_event_listeners())
                    ev.move_from_to_queue(timestamp,veh,veh.my_queue,transit_queue);

            // tell the vehicle it has moved
            veh.move_to_queue(timestamp,transit_queue);

            // tell the travel timers
            if (travel_timer != null)
                ((VehicleLaneGroupTimer)travel_timer).vehicle_enter(timestamp,veh);

            // register_with_dispatcher dispatch to go to waiting queue
            dispatcher.register_event(new EventTransitToWaiting(dispatcher,timestamp + transit_time_sec,veh));

        }

        update_supply();

    }

//    @Override
//    public void exiting_roadconnection_capacity_has_been_modified(float timestamp) {
//
//        // set the capacity of this lanegroup to the minimum of the
//        // exiting road connections
//        current_max_flow_rate_vps = link.outlink2roadconnection.values().stream()
//                .map(x->x.external_max_flow_vps)
//                .min(Float::compareTo)
//                .get();
//
//        current_max_flow_rate_vps = current_max_flow_rate_vps>saturation_flow_rate_vps ?
//                saturation_flow_rate_vps :
//                current_max_flow_rate_vps;
//
//        // Remove scheduled future releases from the previous RC capacity,
//        // will be replaced by new schedule with modified capacity.
//        link.network.scenario.dispatcher
//                .remove_events_for_recipient(EventReleaseVehicleFromLaneGroup.class, this);
//
//        // schedule a release for now+ half wait time
//        schedule_release_vehicle(timestamp,current_max_flow_rate_vps*2);
//
//    }

    /**
     * An event signals an opportunity to release a vehicle. The lanegroup must,
     * 1. construct packets to be released to each of the lanegroups reached by each of it's
     *    road connections.
     * 2. check what portion of each of these packets will be accepted. Reduce the packets
     *    if necessary.
     * 3. call add_vehicle_packet for each reduces packet.
     * 4. remove the vehicle packets from this lanegroup.
     */
    @Override
    public void release_vehicle_packets(float timestamp) throws OTMException {

        // schedule the next vehicle release dispatch
        schedule_release_vehicle(timestamp);

        // ignore if waiting queue is empty
        if(waiting_queue.num_vehicles()==0)
            return;

        // otherwise get the first vehicle
        MesoVehicle vehicle = waiting_queue.peek_vehicle();

        // is this vehicle waiting to change lanes out of its queue?
        // if so, the lane group is blocked
        if(vehicle.waiting_for_lane_change)
            return;

        if(link.is_sink) {

            waiting_queue.remove_given_vehicle(timestamp, vehicle);  // or zero?

            // inform vehicle listener
            if(vehicle.get_event_listeners()!=null)
                for(InterfaceVehicleListener ev : vehicle.get_event_listeners())
                    ev.move_from_to_queue(timestamp,vehicle,waiting_queue,null);

            // inform the travel timers
            if (travel_timer != null)
                ((VehicleLaneGroupTimer)travel_timer).vehicle_exit(timestamp,vehicle,link.getId(),null);

        }
        else{

            // get next link
            State state = vehicle.get_state();
            Long next_link_id = state.isPath ? link.path2outlink.get(state.pathOrlink_id).getId() : state.pathOrlink_id;

            // vehicle should be in a target lane group
            assert(outlink2roadconnection.containsKey(next_link_id));

            RoadConnection rc = outlink2roadconnection.get(next_link_id);
            Link next_link = rc.end_link;

            // at least one candidate lanegroup must have space for one vehicle.
            // Otherwise the road connection is blocked.
            OptionalDouble next_supply_o = rc.out_lanegroups.stream()
                    .mapToDouble(AbstractLaneGroup::get_supply)
                    .max();

            if(!next_supply_o.isPresent())
                return;

            double next_supply = next_supply_o.getAsDouble();

            // release the vehicle if
            // a) connected to a vehicle model and space >= 1
            // b) connected to a fluid model and space >= 0
            if(next_supply > OTMUtils.epsilon){
//            if(    ((next_link.model instanceof AbstractVehicleModel) && next_supply >= 1d)
//                || ((next_link.model instanceof AbstractFluidModel)   && next_supply > OTMUtils.epsilon ) ) {

                // remove vehicle from this lanegroup
                waiting_queue.remove_given_vehicle(timestamp,vehicle);

                // inform the travel timers
                if (travel_timer != null)
                    ((VehicleLaneGroupTimer)travel_timer).vehicle_exit(timestamp,vehicle,link.getId(),next_link);

                // send vehicle packet to next link
                next_link.model.add_vehicle_packet(next_link,timestamp,new PacketLink(vehicle,rc));

                // TODO Need a better solution than this.
                // TODO This is adhoc for when the next links is a fluid model.
                // Todo Then the event counter is not getting triggered.
                // inform the queue counters
                if( !(next_link.model instanceof ModelSpatialQ) && vehicle.get_event_listeners()!=null) {
                    for (InterfaceVehicleListener ev : vehicle.get_event_listeners())
                        ev.move_from_to_queue(timestamp, vehicle, waiting_queue, null);
                }

            } else { // all targets are blocked
                return;
            }

        }

        // tell the flow accumulators
        update_flow_accummulators(vehicle.get_state(),1f);
        update_supply();

        /** NOTE RESOLVE THIS. NEED TO CHECK
         * a) WHETHER THE NEXT LANE GROUP IS MACRO OR MESO.
         * b) IF MACRO, INCREMENT SOME DEMAND BUFFER
         * c) IF MESO, CHECK IF THE NEXT LANE GROUP HAS SPACE. IF IT DOES NOT THEN
         * WHAT TO DO?
         * PERHAPS HAVE ANOTHER QUEUE WHERE VEHICLES WAIT FOR SPACE TO OPEN.
         * HOW DOES THIS WORK WITH CAPACITY?
         */

    }

    @Override
    public float vehs_dwn_for_comm(Long c){
        return (float) (transit_queue.num_vehicles_for_commodity(c) + waiting_queue.num_vehicles_for_commodity(c));
    }

    ///////////////////////////////////////////////////
    // private
    ///////////////////////////////////////////////////

    private void schedule_release_vehicle(float nowtime){

        Float wait_time = OTMUtils.get_waiting_time(saturation_flow_rate_vps,link.model.stochastic_process);

        if(wait_time!=null){
            Scenario scenario = link.network.scenario;
            float timestamp = nowtime + wait_time;
            scenario.dispatcher.register_event(
                    new EventReleaseVehicleFromLaneGroup(scenario.dispatcher,timestamp,this));
        }
    }

}
