/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.ctm;

import keys.KeyCommPathOrLink;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UpLaneGroup {

    public class RcInfo {
        public final RoadConnection rc;
        public Set<KeyCommPathOrLink> S_ir = new HashSet<>();
        public double d_ir;
        public RcInfo(RoadConnection rc) {
            this.rc = rc;
        }
        public void reset(){
            d_ir = S_ir.stream().mapToDouble(state->state_infos.get(state).d_is).sum();
        }
        public void add_state(KeyCommPathOrLink state){
            S_ir.add(state);
            rc.add_state(state);
        }
    }

    public class StateInfo {
        public final KeyCommPathOrLink state;
        public double d_is;
        public double delta_is;
        public StateInfo(KeyCommPathOrLink state){
            this.state = state;
        }
        public void reset(){
            d_is = lg.get_demand_in_target_for_state(state);
            delta_is = Double.NaN;
        }
    }

    public models.ctm.LaneGroup lg;

    public boolean is_empty_or_blocked;
    public double gamma_i;
    public Map<KeyCommPathOrLink,StateInfo> state_infos;
    public Map<KeyCommPathOrLink,Double> f_is;
    public Map<Long,RcInfo> rc_infos;

    ////////////////////////////////////////////
    // construction
    ////////////////////////////////////////////

    public UpLaneGroup(models.ctm.LaneGroup lg){
        this.lg = lg;
        this.is_empty_or_blocked = false;
        this.gamma_i = Double.NaN;
        this.state_infos = new HashMap<>();
        this.f_is = new HashMap<>();
        this.rc_infos = new HashMap<>();
    }

    public void add_road_connection(RoadConnection rc){
        rc_infos.put(rc.id,new RcInfo(rc));
    }

    public void add_state(KeyCommPathOrLink state){
        state_infos.put(state,new StateInfo(state));
        f_is.put(state,0d);

        // S_ir
        Long rc_id = lg.state2roadconnection.get(state);
        if(rc_id!=null)
            rc_infos.get(rc_id).add_state(state);
    }

    ////////////////////////////////////////////
    // update
    ////////////////////////////////////////////

    public void reset(){
        is_empty_or_blocked = false;
        gamma_i = Double.NaN;

        // d_is
        state_infos.values().forEach(x->x.reset());
        f_is.keySet().forEach(x->f_is.put(x,0d));

        // d_ir
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
        return rc_infos.values().stream().mapToDouble(x->x.d_ir).sum();
    }


}
