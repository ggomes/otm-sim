package models;

import common.AbstractVehicle;
import common.Link;
import common.RoadConnection;
import error.OTMException;
import geometry.FlowPosition;
import geometry.Side;
import packet.*;
import runner.RunParameters;
import runner.Scenario;

import java.util.HashSet;
import java.util.Set;

public class VehicleLaneGroup extends BaseLaneGroup {

    public VehicleLaneGroup(Link link, Side side, FlowPosition flwpos, float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs) {
        super(link, side, flwpos, length, num_lanes, start_lane, out_rcs);
    }

    @Override
    public void initialize(Scenario scenario, RunParameters runParams) throws OTMException {
        super.initialize(scenario, runParams);
    }

    @Override
    public float vehs_out_for_comm(Long comm_id) {
        System.err.println("NOT IMPLEMENTED");
        return Float.NaN;
    }

    protected Set<AbstractVehicle> create_vehicles_from_packet(PacketLaneGroup vp,Long next_link_id) {
        // + The packet received here can be fluid or vehicle based. It will not be both
        // because LaneGroupPackets are already model specific, in the sense of either
        // fluid or vehicle based
        // + All of the keys in the packet should be updated using next_link_id.

        assert(vp.vehicles.size()<2);
        assert(vp.vehicles.isEmpty() || vp.container.isEmpty());

        Set<AbstractVehicle> vehs = new HashSet<>();

        VehicleModel model = (VehicleModel) link.model;

        // process 'vehicle' part
        if(!vp.vehicles.isEmpty())
            for(AbstractVehicle abs_veh : vp.vehicles)
                vehs.add(model.translate_vehicle(abs_veh));

        // process 'fluid' part
        if(buffer!=null && !vp.container.isEmpty())
            vehs.addAll( buffer.add_packet_and_extract_vehicles(vp.container,this) );

        // set next link id
        vehs.forEach(v->v.set_next_link_id(next_link_id));

        return vehs;
    }

//    @Override
//    public float get_current_travel_time() {
//        System.err.println("NOT IMPLEMENTED");
//        return Float.NaN;
//    }

    @Override
    public void exiting_roadconnection_capacity_has_been_modified(float timestamp) {
        System.err.println("NOT IMPLEMENTED");
    }

    @Override
    public float vehs_in_for_comm(Long comm_id) {
        System.err.println("NOT IMPLEMENTED");
        return Float.NaN;
    }


}
