package models;

import common.Link;
import common.RoadConnection;
import error.OTMException;
import geometry.FlowDirection;
import geometry.Side;
import packet.AbstractPacketLaneGroup;
import packet.PartialVehicleMemory;
import runner.RunParameters;
import runner.Scenario;

import java.util.Set;

public class AbstractLaneGroupVehicles extends AbstractLaneGroup {

    private PartialVehicleMemory pvm;

    public AbstractLaneGroupVehicles(Link link, Side side, FlowDirection flwdir, float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs) {
        super(link, side, flwdir, length, num_lanes, start_lane, out_rcs);
    }

    @Override
    public void initialize(Scenario scenario, RunParameters runParams) throws OTMException {
        super.initialize(scenario, runParams);
        this.pvm = new PartialVehicleMemory(scenario.commodities);
    }

    @Override
    public double get_supply() {
        return get_space();
    }

    @Override
    public float get_current_travel_time() {
        return Float.NaN;
    }

    @Override
    public void set_max_density_vpkpl(Float max_density_vpkpl) throws OTMException {
        throw new OTMException("NOT IMPLEMENTED");
    }

    @Override
    public void set_max_flow_vpspl(Float max_flow_vpspl) throws OTMException {
        throw new OTMException("NOT IMPLEMENTED");
    }



    @Override
    public void add_native_vehicle_packet(float timestamp, AbstractPacketLaneGroup vp) throws OTMException {

    }

    @Override
    public void exiting_roadconnection_capacity_has_been_modified(float timestamp) {

    }

    @Override
    public void set_max_speed_mps(Float max_speed_mps) throws OTMException {
        throw new OTMException("NOT IMPLEMENTED");
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
}
