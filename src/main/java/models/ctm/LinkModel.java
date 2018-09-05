package models.ctm;

import commodity.Commodity;
import commodity.Subnetwork;
import common.*;
import error.OTMErrorLog;
import error.OTMException;
import keys.KeyCommPathOrLink;
import models.pq.Queue;
import packet.AbstractPacketLaneGroup;
import packet.PacketLink;
import runner.Scenario;
import utils.OTMUtils;

import java.util.*;

public class LinkModel extends AbstractLinkModel {

    public float ff_travel_time_sec;       // free flow travel time in seconds
    private float cell_length_meters;
    private float capacity_vps;
    private int cells_per_lanegroup;

    ////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public LinkModel(common.Link link){
        super(link);
        myPacketClass = models.ctm.PacketLaneGroup.class;
    }

    ////////////////////////////////////////////
    // public
    ///////////////////////////////////////////

    public void create_cells(float max_cell_length){

        // assume that all cells have full lanes (FIX THIS LATER)
//        int lanes = link.full_lanes;

        // construct cells
        float r = link.length/max_cell_length;
        boolean is_source_or_sink = link.is_source || link.is_sink;

        cells_per_lanegroup = is_source_or_sink ?
                1 :
                OTMUtils.approximately_equals(r%1.0,0.0) ? (int) r :  1+((int) r);
        cell_length_meters = is_source_or_sink ?
                link.length :
                link.length/cells_per_lanegroup;

        // create cells
        for(AbstractLaneGroup lg : link.lanegroups.values())
            ((models.ctm.LaneGroup)lg).create_cells(cells_per_lanegroup,cell_length_meters);
    }

    public void register_commodity(Commodity comm,Subnetwork subnet) throws OTMException {

        if(comm.pathfull) {
            KeyCommPathOrLink state = new KeyCommPathOrLink(comm.getId(), subnet.getId(), true);
            for (AbstractLaneGroup lg : link.lanegroups.values()) {
                models.ctm.LaneGroup ctm_lg = (models.ctm.LaneGroup) lg;
                ctm_lg.add_key(state);
            }
        }

        else {

            // for pathless/sink, next link id is same as this id
            if (link.is_sink) {
                KeyCommPathOrLink state = new KeyCommPathOrLink(comm.getId(), link.getId(), false);
                for (AbstractLaneGroup lg : link.lanegroups.values()) {
                    models.ctm.LaneGroup ctm_lg = (models.ctm.LaneGroup) lg;
                    ctm_lg.add_key(state);
                }

            } else {

                // for pathless non-sink, add a state for each next link in the subnetwork
                for (AbstractLaneGroup lg : link.lanegroups.values()) {
                    models.ctm.LaneGroup ctm_lg = (models.ctm.LaneGroup) lg;
                    for (Long next_link_id : lg.get_dwn_links())
                        if (subnet.has_link_id(next_link_id))
                            ctm_lg.add_key(new KeyCommPathOrLink(comm.getId(), next_link_id, false));
                }

            }
        }

    }

    ////////////////////////////////////////////
    // implementation
    ///////////////////////////////////////////

    @Override
    public void set_road_param(jaxb.Roadparam r, float sim_dt_sec) {

        if(Float.isNaN(sim_dt_sec))
            return;

        // adjustment for MN model
        if(link.model_type==Link.ModelType.mn){
            r.setJamDensity(Float.POSITIVE_INFINITY);
        }

        this.capacity_vps = r.getCapacity()/3600f;

        // normalize
        float dt_hr = sim_dt_sec/3600f;
        float capacity_vehperlane = r.getCapacity()*dt_hr;
        float jam_density_vehperlane = r.getJamDensity() * cell_length_meters / 1000f;
        float ffspeed_veh = 1000f * r.getSpeed()*dt_hr / cell_length_meters;

        for(AbstractLaneGroup lg : link.lanegroups.values()) {
            lg.set_road_params(r);
            ((models.ctm.LaneGroup) lg).cells.forEach(c -> c.set_road_params(capacity_vehperlane, jam_density_vehperlane, ffspeed_veh));
        }

        ff_travel_time_sec = 3.6f * link.length / r.getSpeed();
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        if(ff_travel_time_sec<=0)
            errorLog.addError("ff_travel_time_sec<=0");
        if(cell_length_meters<=0)
            errorLog.addError("cell_length_meters<=0");
        if(capacity_vps<=0)
            errorLog.addError("capacity_vps<=0");
        if(cells_per_lanegroup<=0)
            errorLog.addError("cells_per_lanegroup<=0");
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        // allocate state for each lanegroup in this link
        for(AbstractLaneGroup lg : link.lanegroups.values() ){
            ((models.ctm.LaneGroup) lg).allocate_state();
        }
    }

    @Override
    public void reset() {
        for(AbstractLaneGroup alg : link.lanegroups.values()){
            models.ctm.LaneGroup lg = (models.ctm.LaneGroup) alg;
            lg.cells.forEach(x->x.reset());
            lg.flow_notin_target = null;
            lg.flow_notin_target = null;
        }
    }

    @Override
    public float get_ff_travel_time() {
        return ff_travel_time_sec;
    }

    @Override
    public float get_capacity_vps() {
        return capacity_vps;
    }

    ////////////////////////////////////////////
    // update
    ///////////////////////////////////////////

    // call update_supply_demand on each cell
    public void update_lane_changes() {
        for(AbstractLaneGroup lg : link.lanegroups.values()) {
            models.ctm.LaneGroup ctmlg = (models.ctm.LaneGroup) lg;
            if(!ctmlg.states.isEmpty())
                ctmlg.cells.forEach(cell -> cell.update_lane_change_flow());
        }
    }

    public void intermediate_state_update(){
        for(AbstractLaneGroup lg : link.lanegroups.values()) {
            models.ctm.LaneGroup ctmlg = (models.ctm.LaneGroup) lg;
            if(!ctmlg.states.isEmpty())
                ctmlg.cells.forEach(cell -> cell.intermediate_state_update());
        }
    }

    // call update_supply_demand on each cell
    public void update_supply_demand() {
        for(AbstractLaneGroup lg : link.lanegroups.values()) {
            models.ctm.LaneGroup ctmlg = (models.ctm.LaneGroup) lg;
            if(!ctmlg.states.isEmpty())
                ctmlg.cells.forEach(cell -> cell.update_supply_demand());
        }
    }

    public void update_cell_boundary_flows() {
        for(AbstractLaneGroup lg : link.lanegroups.values())
            ((LaneGroup) lg).update_cell_boundary_flows();
    }

    public void update_state() {
        for(AbstractLaneGroup lg : link.lanegroups.values())
            ((models.ctm.LaneGroup) lg).update_state();
    }

//    ////////////////////////////////////////////
//    // get
//    ///////////////////////////////////////////
//
//    public Cell get_upstream_cell_for_lanegroup(Long lg_id){
//        if(link.lanegroups.containsKey(lg_id)) {
//            models.ctm.LaneGroup lg = ((models.ctm.LaneGroup) link.lanegroups.get(lg_id));
//            return lg.cells.get(0);
//        } else {
//            return null;
//        }
//    }
//
//    public Cell get_dnstream_cell_for_lanegroup(Long lg_id){
//        if(link.lanegroups.containsKey(lg_id)) {
//            models.ctm.LaneGroup lg = ((models.ctm.LaneGroup) link.lanegroups.get(lg_id));
//            return lg.cells.get(lg.cells.size()-1);
//        } else {
//            return null;
//        }
//    }
//
//    /** arrange demand into array according to commodity id in clist */
//    public Double get_demand_for_lg_comm_path_rc(Long lg_id, KeyCommodityPathRoadconnection comm_path_rc){
//        Cell dn_cell = get_dnstream_cell_for_lanegroup(lg_id);
//        return dn_cell.demand_in_target.get(comm_path_rc);
//    }
//
//    public Double get_supply_for_lanegroup(Long lg_id){
//        return get_upstream_cell_for_lanegroup(lg_id).supply;
//    }

}
