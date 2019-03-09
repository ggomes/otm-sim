/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.ctm;

import keys.KeyCommPathOrLink;
import models.NodeModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UpLaneGroup {

    public class RcInfo {
        public final RoadConnection rc;
        public Set<KeyCommPathOrLink> S_gr = new HashSet<>();
        public double d_gr;
        public RcInfo(RoadConnection rc) {
            this.rc = rc;
        }
        public void reset(){
            d_gr = S_gr.stream().mapToDouble(state->state_infos.get(state).d_gs).sum();
        }
        public void add_state(KeyCommPathOrLink state){
            S_gr.add(state);
            rc.add_state(state);
        }
    }

    public class StateInfo {
        public final KeyCommPathOrLink state;
        public double d_gs;
        public double delta_gs;
        public StateInfo(KeyCommPathOrLink state){
            this.state = state;
        }
        public void reset(){
            d_gs = lg.get_demand_in_target_for_state(state);
            delta_gs = Double.NaN;
        }
    }

    public LaneGroup lg;

    public boolean is_empty_or_blocked;
    public double gamma_g;
    public Map<KeyCommPathOrLink,StateInfo> state_infos;
    public Map<KeyCommPathOrLink,Double> f_gs;
    public Map<Long,RcInfo> rc_infos;

    ////////////////////////////////////////////
    // construction
    ////////////////////////////////////////////

    public UpLaneGroup(LaneGroup lg){
        this.lg = lg;
        this.is_empty_or_blocked = false;
        this.gamma_g = Double.NaN;
        this.state_infos = new HashMap<>();
        this.f_gs = new HashMap<>();
        this.rc_infos = new HashMap<>();
    }

    public void add_road_connection(RoadConnection rc){
        rc_infos.put(rc.id,new RcInfo(rc));
    }

    public void add_state(KeyCommPathOrLink state){
        state_infos.put(state,new StateInfo(state));
        f_gs.put(state,0d);

        // S_gr
        Long rc_id = lg.state2roadconnection.get(state);
        if(rc_id!=null && rc_infos.containsKey(rc_id))
            rc_infos.get(rc_id).add_state(state);
    }

    ////////////////////////////////////////////
    // update
    ////////////////////////////////////////////

    public void reset(){
        is_empty_or_blocked = false;
        gamma_g = Double.NaN;

        // d_gs
        state_infos.values().forEach(x->x.reset());
        f_gs.keySet().forEach(x-> f_gs.put(x,0d));

        // d_gr
        rc_infos.values().forEach(x->x.reset());

    }

    public void update_is_empty_or_blocked(){
        if(!is_empty_or_blocked) {
            is_empty_or_blocked =
                    total_demand() < NodeModel.eps ||
                            rc_infos.values().stream().anyMatch(x->x.rc.is_blocked);
        }
    }

    ////////////////////////////////////////////
    // get
    ////////////////////////////////////////////

    public double total_demand(){
        return rc_infos.values().stream().mapToDouble(x->x.d_gr).sum();
    }


}
