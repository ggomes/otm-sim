package models;

import common.AbstractVehicle;
import common.Link;
import common.RoadConnection;
import error.OTMException;
import geometry.FlowDirection;
import geometry.Side;
import packet.*;
import runner.RunParameters;
import runner.Scenario;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractLaneGroupVehicles extends AbstractLaneGroup {

    protected StateContainer container;

    public AbstractLaneGroupVehicles(Link link, Side side, FlowDirection flwdir, float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs) {
        super(link, side, flwdir, length, num_lanes, start_lane, out_rcs);
    }

    @Override
    public void initialize(Scenario scenario, RunParameters runParams) throws OTMException {
        super.initialize(scenario, runParams);
        this.container = new StateContainer();
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

        Set<AbstractVehicle> vehicles = null;
        AbstractVehicleModel model = (AbstractVehicleModel) link.model;

        // process 'vehicle' part
            vehicles = new HashSet<>();
            for(AbstractVehicle abs_veh : vp.vehicles)
                vehicles.add(model.translate_vehicle(abs_veh));


        // process 'fluid' part
        vehicles.addAll( container.process_fluid_packet(vp,this) );

        // set next link id
        vehicles.forEach(v->v.set_next_link_id(next_link_id));

        return vehicles;
    }

    @Override
    public float get_current_travel_time() {
        System.err.println("NOT IMPLEMENTED");
        return Float.NaN;
    }

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
