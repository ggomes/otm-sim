package core;

import error.OTMException;
import core.packet.PacketLaneGroup;

public interface InterfaceLaneGroup {

    void allocate_state();

    Double get_upstream_vehicle_position();
    double get_max_vehicles();
    double get_long_supply();
    double get_lat_supply();

    void update_long_supply();
    void add_vehicle_packet(float timestamp, PacketLaneGroup vp, Long nextlink_id) throws OTMException;
    void set_actuator_capacity_vps(double rate_vps);
    void set_to_nominal_capacity();

    void set_actuator_speed_mps(double speed_mps);
    void set_actuator_allow_comm(boolean allow, Long commid)  throws OTMException ;

    // Return the total number of vehicles in this lane group with the given commodity id.
    // commodity_id==null means return total over all commodities.
    float vehs_dwn_for_comm(Long comm_id);
    float vehs_in_for_comm(Long comm_id);
    float vehs_out_for_comm(Long comm_id);

    // An event signals an opportunity to release a vehicle core.packet. The lanegroup must,
    // 1. construct packets to be released to each of the lanegroups reached by each of it's road connections.
    // 2. check what portion of each of these packets will be accepted. Reduce the packets if necessary.
    // 3. call next_link.add_vehicle_packet for each reduces core.packet.
    // 4. remove the vehicle packets from this lanegroup.
    void release_vehicle_packets(float timestamp) throws OTMException;

}
