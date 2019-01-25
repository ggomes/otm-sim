package models.micro;

import common.AbstractVehicle;
import common.Link;
import common.RoadConnection;
import error.OTMException;
import geometry.FlowDirection;
import geometry.Side;
import models.AbstractLaneGroupVehicles;
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
    public void add_vehicle_packet(float timestamp, AbstractPacketLaneGroup avp, Long next_link_id) throws OTMException {

        for(AbstractVehicle vehicle : create_vehicles_from_packet(avp)){

            vehicles.add((models.micro.Vehicle)vehicle);

            System.out.println(String.format("%.2f %d %d ",timestamp,id,vehicle.getId()));

            // inform the travel timers
            link.travel_timers.forEach(x->x.vehicle_enter(timestamp,vehicle));
        }

    }


}
