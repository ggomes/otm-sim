/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.micro;

import commodity.Commodity;
import common.*;
import error.OTMException;
import geometry.Position;
import geometry.Side;
import packet.AbstractPacketLaneGroup;

import java.util.Set;

public class LaneGroup extends AbstractLaneGroup {

    public LaneGroup(Link link, Side side, float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs) {
        super(link, side,length, num_lanes, start_lane, out_rcs);
    }

    @Override
    public void add_commodity(Commodity commodity) {

    }

    @Override
    public void add_native_vehicle_packet(float timestamp, AbstractPacketLaneGroup vp) throws OTMException {
        System.out.format(timestamp + "\tadd_native_vehicle_packet");
    }

    @Override
    public void exiting_roadconnection_capacity_has_been_modified(float timestamp) {

    }

    @Override
    public void release_vehicle_packets(float timestamp) throws OTMException {
        System.out.format(timestamp + "\trelease_vehicle_packets");
    }

    @Override
    public float vehicles_for_commodity(Long commodity_id) {
        return 0;
    }

    @Override
    public float get_current_travel_time() {
        return Float.NaN;
    }

    @Override
    public double get_supply() {
        return 0;
    }

}
