package models.vehicle.newell;

import core.AbstractVehicle;
import core.Link;
import core.RoadConnection;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Roadparam;
import core.State;
import core.AbstractLaneGroup;
import core.AbstractVehicleModel;
import models.vehicle.VehicleLaneGroup;
import core.packet.PacketLaneGroup;
import core.packet.PacketLink;
import core.Scenario;
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

    public NewellLaneGroup(Link link, core.geometry.Side side, float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs, jaxb.Roadparam rp) {
        super(link, side, length, num_lanes, start_lane, out_rcs, rp);
        vehicles = new ArrayList<>();
    }

    ////////////////////////////////////////////
    // InterfaceScenarioElement-like
    ///////////////////////////////////////////

    @Override
    public void validate_post_init(OTMErrorLog errorLog) {

    }

    public void initialize(Scenario scenario, float start_time) throws OTMException {
        super.initialize(scenario, start_time);

        update_long_supply();
    }

    ////////////////////////////////////////////
    // InterfaceLaneGroup
    ///////////////////////////////////////////

    @Override
    public void set_road_params(Roadparam r) {
        super.set_road_params(r);

        float dt = ((ModelNewell)link.get_model()).dt;

        nom_dc = r.getCapacity() * num_lanes * dt / 3600d;  // [veh]
        nom_dv = r.getSpeed() * dt / 3.6d;      // [m]

        dc = nom_dc;
        dv = nom_dv;

        jam_vehpermeter = r.getJamDensity() * num_lanes / 1000d; // [veh/m]
        dw = dc / (jam_vehpermeter - dc/dv); // [m]
    }

    @Override
    public void set_actuator_capacity_vps(double rate_vps) {
        dc = Math.min( nom_dc, rate_vps * ((ModelNewell)link.get_model()).dt );
        dw = dc / (jam_vehpermeter - dc/dv); // [m]
    }

    @Override
    public void set_actuator_speed_mps(double speed_mps) {
        dv = Math.min( nom_dv, speed_mps * ((ModelNewell)link.get_model()).dt );
        dw = dc / (jam_vehpermeter - dc/dv); // [m]
    }

    @Override
    public void set_actuator_allow_comm(boolean allow, Long commid) {
        System.out.println("NOT IMPLEMENTED!!");
    }

    @Override
    public void allocate_state() {

    }

    @Override
    public void update_long_supply() {
//        supply =  max_vehicles - vehicles.size();

        Double up_veh_pos = get_upstream_vehicle_position();
        long_supply =  up_veh_pos.isNaN() ? max_vehicles : up_veh_pos * max_vehicles / length;

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

        update_long_supply();

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

        double next_supply = Double.POSITIVE_INFINITY;
        Link next_link = null;
        RoadConnection rc = null;

        if(!link.is_sink()){

            // get next link
            State state = vehicle.get_state();
            Long next_link_id = state.isPath ? link.get_next_link_in_path(state.pathOrlink_id).getId() : state.pathOrlink_id;

            rc = outlink2roadconnection.get(next_link_id);
            next_link = rc.get_end_link();

            // at least one candidate lanegroup must have space for one vehicle.
            // Otherwise the road connection is blocked.
            OptionalDouble next_supply_o = rc.get_out_lanegroups().stream()
                    .mapToDouble(AbstractLaneGroup::get_long_supply)
                    .max();

            assert(next_supply_o.isPresent());
            next_supply = next_supply_o.getAsDouble();
        }

        if(next_supply > OTMUtils.epsilon){

            // possibly disconnect from follower
            if(next_link==null || !(next_link.get_model() instanceof AbstractVehicleModel))
                if(vehicle.follower!=null) {
                    vehicle.follower.headway = Double.POSITIVE_INFINITY;
                    vehicle.follower.leader = null;
                }

            // remove the vehicle from the lanegroup
            it.remove();
            vehicle.new_pos -= vehicle.lg.get_length();

            // inform flow accumulators
            update_flow_accummulators(vehicle.get_state(), 1f);

            // inform the travel timers
            if(travel_timer!=null)
                ((VehicleLaneGroupTimer)travel_timer).vehicle_exit(timestamp,vehicle,link.getId(),next_link);

            // send vehicle core.packet to next link
            if(next_link!=null && rc!=null)
                next_link.get_model().add_vehicle_packet(next_link,timestamp,new PacketLink(vehicle,rc));

            update_long_supply();

            return true;
        }

        return false;

    }
}
