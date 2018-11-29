/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.ctm;

import common.*;
import error.OTMErrorLog;
import geometry.Side;
import keys.KeyCommPathOrLink;
import utils.OTMUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LinkModel extends AbstractLinkModel {

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

        // construct cells

        // create cells
        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values()) {

            float r = lg.length/max_cell_length;
            boolean is_source_or_sink = link.is_source || link.is_sink;

            int cells_per_lanegroup = is_source_or_sink ?
                    1 :
                    OTMUtils.approximately_equals(r%1.0,0.0) ? (int) r :  1+((int) r);
            float cell_length_meters = is_source_or_sink ?
                    lg.length :
                    lg.length/cells_per_lanegroup;

            ((LaneGroup) lg).create_cells(cells_per_lanegroup, cell_length_meters);
        }
    }

    ////////////////////////////////////////////
    // implementation
    ///////////////////////////////////////////

    @Override
    public void validate(OTMErrorLog errorLog) {
    }

    @Override
    public void reset() {
        for(AbstractLaneGroup alg : link.lanegroups_flwdn.values()){
            LaneGroup lg = (LaneGroup) alg;
            lg.cells.forEach(x->x.reset());
        }
    }

    @Override
    public Map<AbstractLaneGroup,Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups) {
        Map<AbstractLaneGroup,Double> A = new HashMap<>();
        double total_supply = candidate_lanegroups.stream().mapToDouble(x->x.get_supply()).sum();
        for(AbstractLaneGroup laneGroup : candidate_lanegroups)
            A.put(laneGroup , laneGroup.get_supply() / total_supply);
        return A;
    }

    ////////////////////////////////////////////
    // update
    ///////////////////////////////////////////

    public void perform_lane_changes(float timestamp) {

        // WARNING: THIS ASSUMES NO ADDLANES (lanegroups_flwdn=all lanegroups)

        int cells_in_full_lg = ((LaneGroup)link.lanegroups_flwdn.values().iterator().next()).cells.size();

        // scan cross section from upstream to downstream
        for (int i = 0; i < cells_in_full_lg; i++) {

            Map<Long, Double> gamma = new HashMap<>();

            // compute total flows reduction for each lane group
            for (AbstractLaneGroup lg : link.lanegroups_flwdn.values()) {
                Cell cell = ((LaneGroup) lg).cells.get(i);
                double demand_to_me = 0d;
                if (lg.neighbor_in != null)
                    demand_to_me += ((LaneGroup) lg.neighbor_in).cells.get(i).total_vehs_out;
                if (lg.neighbor_out != null)
                    demand_to_me += ((LaneGroup) lg.neighbor_out).cells.get(i).total_vehs_in;

                // TODO: extract xi as a parameter
                double supply = 0.9d * (1d - cell.wspeed_norm) * (cell.jam_density_veh - cell.get_vehicles());
                gamma.put(lg.id, demand_to_me > supply ? supply / demand_to_me : 1d);
            }

            // lane change flow
            // WARNING: This assumes that no state has vehicles going in both directions.
            // ie a flow that goes left does not also go right. Otherwise I think there may
            // be "data races", where the result depends on the order of lgs.
            for (AbstractLaneGroup alg : link.lanegroups_flwdn.values()) {
                models.ctm.LaneGroup lg = (models.ctm.LaneGroup) alg;
                double my_gamma = gamma.get(lg.id);

                if (lg.neighbor_in != null) {

                    LaneGroup from_lg = (LaneGroup) lg.neighbor_in;
                    Cell from_cell = from_lg.cells.get(i);
                    Map<KeyCommPathOrLink, Double> from_vehs = from_cell.veh_out;

                    ///////////////////////////////////////////////////////////////////////////////////////////

                    for (Map.Entry<KeyCommPathOrLink, Double> e : from_vehs.entrySet()) {
                        Double from_veh = e.getValue();
                        KeyCommPathOrLink state = e.getKey();

                        if (from_veh > OTMUtils.epsilon) {

                            Cell to_cell = lg.cells.get(i);
                            double flw = my_gamma  * from_veh;

                            // remove from this cell
                            from_vehs.put(state, from_veh-flw );
                            from_cell.total_vehs_out -= flw;

                            // add to side cell
                            Side newside = lg.state2lanechangedirection.containsKey(state) ?
                                    lg.state2lanechangedirection.get(state) :
                                    Side.full;
                            switch (newside) {
                                case in:
                                    to_cell.veh_in.put(state, to_cell.veh_in.get(state) + flw);
                                    to_cell.total_vehs_in += flw;
                                    break;
                                case full:
                                    to_cell.veh_dwn.put(state, to_cell.veh_dwn.get(state) + flw);
                                    to_cell.total_vehs_dwn += flw;
                                    break;
                                case out:
                                    to_cell.veh_out.put(state, to_cell.veh_out.get(state) + flw);
                                    to_cell.total_vehs_out += flw;
                                    break;
                            }
                        }
                    }
                    ///////////////////////////////////////////////////////////////////////////////////////////
                }
                if (lg.neighbor_out != null) {

                    LaneGroup from_lg = (LaneGroup) lg.neighbor_out;
                    Cell from_cell = from_lg.cells.get(i);
                    Map<KeyCommPathOrLink, Double> from_vehs = from_cell.veh_in;

                    ///////////////////////////////////////////////////////////////////////////////////////////

                    for (Map.Entry<KeyCommPathOrLink, Double> e : from_vehs.entrySet()) {
                        Double from_veh = e.getValue();
                        KeyCommPathOrLink state = e.getKey();

                        if (from_veh > OTMUtils.epsilon) {

                            Cell to_cell = lg.cells.get(i);
                            double flw = my_gamma * from_veh;

                            // remove from this cell
                            from_vehs.put(state, from_veh-flw );
                            from_cell.total_vehs_in -= flw;

                            // add to side cell
                            Side newside = lg.state2lanechangedirection.containsKey(state) ?
                                    lg.state2lanechangedirection.get(state) :
                                    Side.full;
                            switch (newside) {
                                case in:
                                    to_cell.veh_in.put(state, to_cell.veh_in.get(state) + flw);
                                    to_cell.total_vehs_in += flw;
                                    break;
                                case full:
                                    to_cell.veh_dwn.put(state, to_cell.veh_dwn.get(state) + flw);
                                    to_cell.total_vehs_dwn += flw;
                                    break;
                                case out:
                                    to_cell.veh_out.put(state, to_cell.veh_out.get(state) + flw);
                                    to_cell.total_vehs_out += flw;
                                    break;
                            }
                        }
                    }


                    ///////////////////////////////////////////////////////////////////////////////////////////
                }
            }
        }

    }

    // call update_supply_demand on each cell
    public void update_supply_demand() {
        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values()) {
            LaneGroup ctmlg = (LaneGroup) lg;
            if(!ctmlg.states.isEmpty())
                ctmlg.cells.forEach(cell -> cell.update_supply_demand());
        }
    }

    public void update_dwn_flow() {
        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values())
            ((LaneGroup) lg).update_dwn_flow();
    }

    public void update_state(float timestamp) {
        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values())
            ((LaneGroup) lg).update_state(timestamp);
    }

}
