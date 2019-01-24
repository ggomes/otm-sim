package models.micro;

import common.AbstractVehicle;
import common.Link;
import common.RoadConnection;
import error.OTMException;
import geometry.FlowDirection;
import geometry.Side;
import models.AbstractLaneGroupVehicles;
import models.VehiclePacket;
import packet.AbstractPacketLaneGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LaneGroup extends AbstractLaneGroupVehicles {

    List<models.micro.Vehicle> vehicles;

    public LaneGroup(Link link, Side side, FlowDirection flwdir, float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs) {
        super(link, side, flwdir, length, num_lanes, start_lane, out_rcs);

        vehicles = new ArrayList<>();
    }

    ///////////////////////////////////////////////////
    // AbstractLaneGroup : abstract methods
    ///////////////////////////////////////////////////

    @Override
    public void add_native_vehicle_packet(float timestamp, AbstractPacketLaneGroup vp) throws OTMException {
        Set<AbstractVehicle> vs = ((VehiclePacket)vp).vehicles;
        assert(vs.size()==1);
        vehicles.add((models.micro.Vehicle)vs.iterator().next());
    }

}
