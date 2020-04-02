package models.none;

import common.Link;
import common.RoadConnection;
import error.OTMErrorLog;
import error.OTMException;
import geometry.FlowPosition;
import geometry.Side;
import models.AbstractLaneGroup;
import packet.PacketLaneGroup;

import java.util.Set;

public class NoneLaneGroup extends AbstractLaneGroup {

    @Override
    public void validate(OTMErrorLog errorLog) {

    }

    @Override
    public Double get_upstream_vehicle_position() {
        return null;
    }

    @Override
    public void allocate_state() {

    }

    @Override
    public void update_supply() {

    }

    @Override
    public void add_vehicle_packet(float timestamp, PacketLaneGroup vp, Long nextlink_id) throws OTMException {

    }

    @Override
    public void exiting_roadconnection_capacity_has_been_modified(float timestamp) {

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

    public NoneLaneGroup(Link link, Side side, FlowPosition flwpos, float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs) {
        super(link, side, flwpos, length, num_lanes, start_lane, out_rcs);
    }
}
