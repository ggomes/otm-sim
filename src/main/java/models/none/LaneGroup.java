/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.none;

import common.*;
import error.OTMException;
import geometry.FlowDirection;
import geometry.Side;
import models.AbstractLaneGroup;
import packet.PacketLaneGroup;

import java.util.Set;

public class LaneGroup extends AbstractLaneGroup {

    public LaneGroup(Link link, Side side, FlowDirection flwdir, float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs) {
        super(link, side,flwdir,length, num_lanes, start_lane, out_rcs);
    }

    @Override
    public void exiting_roadconnection_capacity_has_been_modified(float timestamp) {

    }

    @Override
    public void release_vehicle_packets(float timestamp) throws OTMException {

    }

    @Override
    public float vehs_dwn_for_comm(Long commodity_id) {
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

//    @Override
//    public float get_current_travel_time() {
//        return Float.NaN;
//    }

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
    public Double get_upstream_vehicle_position(){
        return Double.NaN;
    }

}
