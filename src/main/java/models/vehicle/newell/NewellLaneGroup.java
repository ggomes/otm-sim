package models.vehicle.newell;

import common.AbstractVehicle;
import common.Link;
import common.RoadConnection;
import error.OTMErrorLog;
import error.OTMException;
import geometry.FlowPosition;
import geometry.Side;
import jaxb.Roadparam;
import keys.State;
import common.AbstractLaneGroup;
import models.vehicle.VehicleLaneGroup;
import packet.PacketLaneGroup;
import packet.PacketLink;
import common.Scenario;
import traveltime.VehicleLaneGroupTimer;
import utils.OTMUtils;

import java.util.*;

public class NewellLaneGroup extends VehicleLaneGroup {

    public List<NewellVehicle> vehicles;

    // nominal parameters
    public double nom_dv;   // vf*dt [meters per dt]
    public double nom_dc;   // rate*dt [veh per dt]
    public double jam_vehpermeter;

    // actual (actuated) fd
    public double dv;   // vf*dt [meters per dt]
    public double dw;   // w*dt [meters per dt]
    public double dc;   // rate*dt [veh per dt]

    ////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public NewellLaneGroup(Link link, Side side, FlowPosition flwpos, float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs, jaxb.Roadparam rp) {
        super(link, side, flwpos, length, num_lanes, start_lane, out_rcs, rp);
        vehicles = new ArrayList<>();
    }

    ////////////////////////////////////////////
    // InterfaceScenarioElement-like
    ///////////////////////////////////////////

    @Override
    public void validate(OTMErrorLog errorLog) {

    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);

        update_supply();
    }

    ////////////////////////////////////////////
    // InterfaceLaneGroup
    ///////////////////////////////////////////

    @Override
    public void set_road_params(Roadparam r) {
        super.set_road_params(r);

        float dt = ((ModelNewell)link.model).dt;

        nom_dc = r.getCapacity() * num_lanes * dt / 3600d;  // [veh]
        nom_dv = r.getSpeed() * dt / 3.6d;      // [m]

        dc = nom_dc;
        dv = nom_dv;

        jam_vehpermeter = r.getJamDensity() * num_lanes / 1000d; // [veh/m]
        dw = dc / (jam_vehpermeter - dc/dv); // [m]
    }

    @Override
    public void set_actuator_capacity_vps(double rate_vps) {
        dc = Math.min( nom_dc, rate_vps * ((ModelNewell)link.model).dt );
        dw = dc / (jam_vehpermeter - dc/dv); // [m]
    }

    @Override
    public void set_actuator_speed_mps(double speed_mps) {
        dv = Math.min( nom_dv, speed_mps * ((ModelNewell)link.model).dt );
        dw = dc / (jam_vehpermeter - dc/dv); // [m]
    }

    @Override
    public void set_actuator_isopen(boolean isopen,Long commid) {
        System.out.println("NOT IMPLEMENTED!!");
    }

    @Override
    public void allocate_state() {

    }

    @Override
    public void update_supply() {
//        supply =  max_vehicles - vehicles.size();

        Double up_veh_pos = get_upstream_vehicle_position();
        supply =  up_veh_pos.isNaN() ? max_vehicles : up_veh_pos * max_vehicles / length;

//        if(link.is_model_source_link)
//            supply = Math.max(0d,supply + 1d - buffer.get_total_veh());

    }

    @Override
    public Double get_upstream_vehicle_position(){
        return vehicles.isEmpty() ? Double.NaN : vehicles.get(vehicles.size()-1).pos;
    }

    @Override
    public void add_vehicle_packet(float timestamp, PacketLaneGroup vp, Long next_link_id) throws OTMException {

        for(AbstractVehicle aveh : create_vehicles_from_packet(vp,next_link_id)){

            NewellVehicle vehicle = (NewellVehicle)aveh;

            vehicle.lg = this;

            if(!vehicles.isEmpty()) {

                NewellVehicle leader = vehicles.get(vehicles.size()-1);
                leader.follower = vehicle;
                vehicle.leader = leader;

                vehicle.new_pos = Math.min( vehicle.new_pos , leader.pos - dw);
                vehicle.new_pos = Math.max( vehicle.new_pos , 0d);
                vehicle.pos = vehicle.new_pos;
                vehicle.headway = leader.pos - vehicle.pos;
            }

            else {
                vehicle.leader = null;
                vehicle.headway = Double.POSITIVE_INFINITY;
            }

            vehicles.add(vehicle);

            // inform the travel timers
            if(travel_timer!=null)
                ((VehicleLaneGroupTimer)travel_timer).vehicle_enter(timestamp,vehicle);

        }

        update_supply();

    }

    @Override
    public float vehs_dwn_for_comm(Long comm_id) {
        return comm_id==null ?
                vehicles.size() :
                vehicles.stream().filter(v->v.get_commodity_id()==comm_id).count();
    }

    @Override
    public void release_vehicle_packets(float timestamp) throws OTMException {
        throw new OTMException("NOT IMPLEMENTED awpirg -jqig");
    }

    ////////////////////////////////////////////
    // InterfaceLaneGroup
    ///////////////////////////////////////////

    protected boolean release_vehicle(float timestamp, Iterator<NewellVehicle> it, NewellVehicle vehicle) throws OTMException {

        boolean released = false;

        if(link.is_sink) {

            if(vehicle.follower!=null) {
                vehicle.follower.leader = null;
                vehicle.follower.headway = Double.POSITIVE_INFINITY;
            }

            // remove the vehicle from the lanegroup
            it.remove();


//            // inform vehicle listener
//            if(vehicle.get_event_listeners()!=null)
//                for(InterfaceVehicleListener ev : vehicle.get_event_listeners())
//                    ev.move_from_to_queue(timestamp,vehicle,waiting_queue,null);

            // inform the travel timers
            if(travel_timer!=null)
                ((VehicleLaneGroupTimer)travel_timer).vehicle_exit(timestamp,vehicle,link.getId(),null);

            released = true;
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

            assert(next_supply_o.isPresent());
//            if(!next_supply_o.isPresent())
//                return false;

            double next_supply = next_supply_o.getAsDouble();

            // release the vehicle if
            // a) connected to a vehicle model and space >= 1
            // b) connected to a fluid model and space >= 0

            if(next_supply > OTMUtils.epsilon){

//                if(    ((next_link.model instanceof AbstractVehicleModel) && next_supply >= 1d)
//                    || ((next_link.model instanceof AbstractFluidModel)   && next_supply > OTMUtils.epsilon ) ) {

                // remove the vehicle from the lanegroup
                it.remove();
                vehicle.new_pos -= vehicle.lg.length;

                // inform the travel timers
                if(travel_timer!=null)
                    ((VehicleLaneGroupTimer)travel_timer).vehicle_exit(timestamp,vehicle,link.getId(),next_link);

                // send vehicle packet to next link
                next_link.model.add_vehicle_packet(next_link,timestamp,new PacketLink(vehicle,rc));

                // possibly disconnect from follower
                if(!(next_link.model instanceof ModelNewell) && vehicle.follower!=null)
                    vehicle.follower.leader = null;

                released = true;
            }

        }

        // tell the flow accumulators
        if(released) {
            update_flow_accummulators(vehicle.get_state(), 1f);
            update_supply();
        }

        return released;

        /** NOTE RESOLVE THIS. NEED TO CHECK
         * a) WHETHER THE NEXT LANE GROUP IS MACRO OR MESO.
         * b) IF MACRO, INCREMENT SOME DEMAND BUFFER
         * c) IF MESO, CHECK IF THE NEXT LANE GROUP HAS SPACE. IF IT DOES NOT THEN
         * WHAT TO DO?
         * PERHAPS HAVE ANOTHER QUEUE WHERE VEHICLES WAIT FOR SPACE TO OPEN.
         * HOW DOES THIS WORK WITH CAPACITY?
         */


    }
}
