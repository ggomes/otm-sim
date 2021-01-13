package models.none;

import core.Link;
import core.RoadConnection;
import error.OTMErrorLog;
import error.OTMException;
import core.AbstractLaneGroup;
import jaxb.Roadparam;
import core.packet.PacketLaneGroup;

import java.util.Set;

public class NoneLaneGroup extends AbstractLaneGroup {

    @Override
    public void validate_post_init(OTMErrorLog errorLog) {

    }

    @Override
    public void set_actuator_capacity_vps(double rate_vps) {

    }

    @Override
    public void set_actuator_allow_comm(boolean allow, Long commid) {
        System.out.println("NOT IMPLEMENTED!!");
    }


    @Override
    public Double get_upstream_vehicle_position() {
        return null;
    }

    @Override
    public double get_max_vehicles() {
        return 0;
    }

    @Override
    public void allocate_state() {

    }

    @Override
    public double get_lat_supply(){
        return 0d;
    }

    @Override
    public void update_long_supply() {

    }

    @Override
    public void add_vehicle_packet(float timestamp, PacketLaneGroup vp, Long nextlink_id) throws OTMException {

    }


    @Override
    public void set_actuator_speed_mps(double speed_mps) {
    }

    @Override
    public void set_road_params(Roadparam r) {

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
    public void release_vehicle_packets(float timestamp) throws OTMException {

    }

    public NoneLaneGroup(Link link, core.geometry.Side side, float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs, jaxb.Roadparam rp) {
        super(link, side, length, num_lanes, start_lane, out_rcs, rp);
    }
}
