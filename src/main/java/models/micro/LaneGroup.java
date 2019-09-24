package models.micro;

import common.AbstractVehicle;
import common.Link;
import common.RoadConnection;
import error.OTMException;
import geometry.FlowPosition;
import geometry.Side;
import jaxb.Roadparam;
import keys.KeyCommPathOrLink;
import models.AbstractLaneGroup;
import models.AbstractLaneGroupVehicles;
import packet.PacketLaneGroup;
import packet.PacketLink;
import runner.RunParameters;
import runner.Scenario;
import traveltime.VehicleLaneGroupTimer;
import utils.OTMUtils;

import java.util.*;

public class LaneGroup extends AbstractLaneGroupVehicles {

    public List<models.micro.Vehicle> vehicles;
    public double dv;   // vf*dt [meters per dt]
    public double dw;   // w*dt [meters per dt]
    public double dc;   // capcity*dt [veh per dt]

    public LaneGroup(Link link, Side side, FlowPosition flwpos, float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs) {
        super(link, side, flwpos, length, num_lanes, start_lane, out_rcs);
        vehicles = new ArrayList<>();
    }

    ///////////////////////////////////////////////////
    // load
    ///////////////////////////////////////////////////

    @Override
    public void initialize(Scenario scenario, RunParameters runParams) throws OTMException {
        super.initialize(scenario, runParams);

        update_supply();
    }

    @Override
    public void set_road_params(Roadparam r) {
        super.set_road_params(r);
        dv = r.getSpeed() * ((Model_Micro)link.model).dt / 3.6d;
        dw = max_cong_speed_kph * ((Model_Micro)link.model).dt / 3.6d;
        dc = r.getCapacity() * num_lanes * ((Model_Micro)link.model).dt / 3600d;
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

    ///////////////////////////////////////////////////
    // run
    ///////////////////////////////////////////////////

    @Override
    public Double get_upstream_vehicle_position(){
        return vehicles.isEmpty() ? Double.NaN : vehicles.get(vehicles.size()-1).pos;
    }

    @Override
    public void add_vehicle_packet(float timestamp, PacketLaneGroup vp, Long next_link_id) throws OTMException {

        for(AbstractVehicle aveh : create_vehicles_from_packet(vp,next_link_id)){

            models.micro.Vehicle vehicle = (models.micro.Vehicle)aveh;

            vehicle.lg = this;

            if(!vehicles.isEmpty()) {

                Vehicle leader = vehicles.get(vehicles.size()-1);
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

    public boolean release_vehicle(float timestamp, Iterator<Vehicle> it,Vehicle vehicle) throws OTMException {

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
            ((VehicleLaneGroupTimer)travel_timer).vehicle_exit(timestamp,vehicle,link.getId(),null);

            released = true;
        }
        else{

            // get next link
            KeyCommPathOrLink key = vehicle.get_key();
            Long next_link_id = key.isPath ? link.path2outlink.get(key.pathOrlink_id).getId() : key.pathOrlink_id;

            // vehicle should be in a target lane group
            assert(link.outlink2roadconnection.containsKey(next_link_id));

            RoadConnection rc = link.outlink2roadconnection.get(next_link_id);
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
                ((VehicleLaneGroupTimer)travel_timer).vehicle_exit(timestamp,vehicle,link.getId(),next_link);

                // send vehicle packet to next link
                next_link.model.add_vehicle_packet(next_link,timestamp,new PacketLink(vehicle,rc));

                // possibly disconnect from follower
                if(!(next_link.model instanceof models.micro.Model_Micro) && vehicle.follower!=null)
                    vehicle.follower.leader = null;

                released = true;
            }

        }

        // tell the flow accumulators
        if(released) {
            update_flow_accummulators(vehicle.get_key(), 1f);
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
