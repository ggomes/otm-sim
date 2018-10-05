package models.none;

import commodity.Commodity;
import common.AbstractLaneGroupLateral;
import common.Link;
import error.OTMException;
import geometry.Side;
import keys.KeyCommPathOrLink;
import packet.AbstractPacketLaneGroup;

public class LaneGroupLat extends AbstractLaneGroupLateral {

    public LaneGroupLat(Link link, Side side, float length, int num_lanes, int start_lane) {
        super(link, side, length, num_lanes, start_lane);
    }

    @Override
    public void add_commodity(Commodity commodity) {

    }

    @Override
    public void add_native_vehicle_packet(float timestamp, AbstractPacketLaneGroup vp) throws OTMException {

    }

    @Override
    public double get_supply() {
        return 0;
    }

    @Override
    public float vehicles_for_commodity(Long commodity_id) {
        return 0;
    }

    @Override
    public float get_current_travel_time() {
        return 0;
    }

    @Override
    public void allocate_state() {

    }

    @Override
    public void add_key(KeyCommPathOrLink state) {

    }
}
