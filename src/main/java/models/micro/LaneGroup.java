package models.micro;

import common.AbstractVehicle;
import common.Link;
import common.RoadConnection;
import error.OTMException;
import geometry.FlowDirection;
import geometry.Side;
import jaxb.Roadparam;
import keys.KeyCommPathOrLink;
import models.AbstractFluidModel;
import models.AbstractLaneGroup;
import models.AbstractLaneGroupVehicles;
import models.AbstractVehicleModel;
import output.InterfaceVehicleListener;
import packet.AbstractPacketLaneGroup;
import packet.PacketLink;
import runner.RunParameters;
import runner.Scenario;
import utils.OTMUtils;

import java.util.*;

public class LaneGroup extends AbstractLaneGroupVehicles {

    public List<models.micro.Vehicle> vehicles;
    public double dv;   // vf*dt [meters]
    public double dw;   // w*DT [meters]

    public LaneGroup(Link link, Side side, FlowDirection flwdir, float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs) {
        super(link, side, flwdir, length, num_lanes, start_lane, out_rcs);
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
    }

    @Override
    public void allocate_state() {

    }

    @Override
    public void update_supply() {
        supply =  max_vehicles - get_total_vehicles();
    }

    ///////////////////////////////////////////////////
    // run
    ///////////////////////////////////////////////////

    @Override
    public Double get_upstream_vehicle_position(){
        return vehicles.isEmpty() ? Double.NaN : vehicles.get(vehicles.size()-1).pos;
    }

    @Override
    public void add_vehicle_packet(float timestamp, AbstractPacketLaneGroup avp, Long next_link_id) throws OTMException {

        for(AbstractVehicle aveh : create_vehicles_from_packet(avp,next_link_id)){

            models.micro.Vehicle vehicle = (models.micro.Vehicle)aveh;

            Vehicle leader = null;

            if(!vehicles.isEmpty()) {
                leader = vehicles.get(vehicles.size()-1);
                vehicle.leader = leader;
                leader.follower = vehicle;
            }

            vehicle.lg = this;
            vehicle.headway = Vehicle.initialize_headway(leader,vehicle);
            vehicles.add(vehicle);


            // inform the travel timers
            link.travel_timers.forEach(x->x.vehicle_enter(timestamp,vehicle));
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


    public void release_vehicle(float timestamp, Iterator<Vehicle> it,Vehicle vehicle) throws OTMException {

        if(link.is_sink) {

            it.remove();

//            // inform vehicle listener
//            if(vehicle.get_event_listeners()!=null)
//                for(InterfaceVehicleListener ev : vehicle.get_event_listeners())
//                    ev.move_from_to_queue(timestamp,vehicle,waiting_queue,null);

            // inform the travel timers
            link.travel_timers.forEach(x->x.vehicle_exit(timestamp,vehicle,link.getId(),null));

        }
        else{

            // get next link
            KeyCommPathOrLink key = vehicle.get_key();
            Long next_link_id = key.isPath ? link.path2outlink.get(key.pathOrlink_id).getId() : key.pathOrlink_id;

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
            if(    ((next_link.model instanceof AbstractVehicleModel) && next_supply >= 1d)
                    || ((next_link.model instanceof AbstractFluidModel)   && next_supply > OTMUtils.epsilon ) ) {

                // remove vehicle from this lanegroup
                it.remove();

                // inform the travel timers
                link.travel_timers.forEach(x->x.vehicle_exit(timestamp,vehicle,link.getId(),next_link));

                // send vehicle packet to next link
                next_link.model.add_vehicle_packet(next_link,timestamp,new PacketLink(vehicle,rc));

            } else { // all targets are blocked
                return;
            }

        }

        // tell the flow accumulators
        update_flow_accummulators(vehicle.get_key(),1f);
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
}
