package models.micro;

import commodity.Commodity;
import common.AbstractVehicle;
import common.Link;
import common.RoadConnection;
import error.OTMException;
import geometry.FlowDirection;
import geometry.Side;
import models.AbstractLaneGroup;
import models.VehiclePacket;
import packet.AbstractPacketLaneGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LaneGroup extends AbstractLaneGroup {

    List<models.micro.Vehicle> vehicles;

    public LaneGroup(Link link, Side side, FlowDirection flwdir, float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs) {
        super(link, side, flwdir, length, num_lanes, start_lane, out_rcs);

        vehicles = new ArrayList<>();
    }

    ///////////////////////////////////////////////////
    // AbstractLaneGroup : abstract methods
    ///////////////////////////////////////////////////

    @Override
    public double get_supply() {
        return 0;
    }

    @Override
    public void add_native_vehicle_packet(float timestamp, AbstractPacketLaneGroup vp) throws OTMException {
        Set<AbstractVehicle> vs = ((VehiclePacket)vp).vehicles;
        assert(vs.size()==1);
        vehicles.add((models.micro.Vehicle)vs.iterator().next());
    }

//    @Override
//    public void add_commodity(Commodity commodity) {
//
//    }

    @Override
    public void exiting_roadconnection_capacity_has_been_modified(float timestamp) {

    }

    @Override
    public void set_max_speed_mps(Float max_speed_mps) throws OTMException {

    }

    @Override
    public void set_max_flow_vpspl(Float max_flow_vpspl) throws OTMException {

    }

    @Override
    public void set_max_density_vpkpl(Float max_density_vpkpl) throws OTMException {

    }

    @Override
    public float vehs_dwn_for_comm(Long comm_id) {
        return 0;
    }

    @Override
    public float vehs_in_for_comm(Long comm_id) {
        return 0;
    }

    @Override
    public float vehs_out_for_comm(Long comm_id) {
        return 0;
    }

    @Override
    public float get_current_travel_time() {
        return 0;
    }

    @Override
    public void release_vehicle_packets(float timestamp) throws OTMException {

    }
}
