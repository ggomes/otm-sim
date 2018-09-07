/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.ctm;

import keys.KeyCommPathOrLink;

import java.util.HashMap;
import java.util.Map;

public class UpLaneGroup {

    public models.ctm.LaneGroup lg;

    public boolean is_empty_or_blocked;
    public double gamma_i;

    public Map<Long,RoadConnection> rcs;
    public Map<Long,Double> d_ir;                       // rc -> d_r

    public Map<KeyCommPathOrLink,Double> delta_icp;     // state -> flow bit  [PROBABLY REMOVE]
    public Map<KeyCommPathOrLink,Double> f_icp;         // state -> flow
    public Map<KeyCommPathOrLink,Double> eta_icp;       // state -> proportion of d_r
    public Map<KeyCommPathOrLink,Double> d_icp;         // state -> d_r

    public UpLaneGroup(models.ctm.LaneGroup lg){
        this.lg = lg;
        this.rcs = new HashMap<>();
        this.is_empty_or_blocked = false;
        this.d_ir = new HashMap<>();
        this.gamma_i = Double.NaN;
        this.eta_icp = new HashMap<>();
        this.delta_icp = new HashMap<>();
        this.f_icp = new HashMap<>();
        this.d_icp = new HashMap<>();
    }

    public void reset(){
        is_empty_or_blocked = false;
        f_icp.keySet().forEach(key->f_icp.put(key,0d));
        d_ir.keySet().forEach(key->d_ir.put(key,0d));
    }

    public void add_road_connection(RoadConnection rc){
        rcs.put(rc.id,rc);
        d_ir.put(rc.id,0d);
    }

    public void add_state(KeyCommPathOrLink state){
        d_icp.put(state,0d);
        eta_icp.put(state,0d);
        delta_icp.put(state,0d);
        f_icp.put(state,0d);

        Long rc_id = lg.state2roadconnection.get(state);
        if(rc_id!=null)
            rcs.get(rc_id).add_state(state);
    }

    public double total_demand(){
        return d_ir.values().stream().reduce(0.0, Double::sum);
    }

    public void update_is_empty_or_blocked(){
        if(!is_empty_or_blocked) {
            is_empty_or_blocked =
                    total_demand() < NodeModel.eps ||
                    rcs.values().stream().anyMatch(rc->rc.is_blocked);
            if(is_empty_or_blocked)
                gamma_i = Double.POSITIVE_INFINITY;
        }
    }

    public double get_total_demand(){
        return d_ir.values().stream().mapToDouble(x->x).sum();
    }

    public double get_flow_for_commodity(Long comm_id){
        return f_icp.keySet().stream()
                .filter(key->key.commodity_id==comm_id)
                .mapToDouble(key->f_icp.get(key))
                .sum();
    }

    @Override
    public String toString() {
        String str = "";
        str += String.format("ulg: link %d, ",lg.link.getId());
        str += "rcs=[";
        for(Long rcid : rcs.keySet())
            str += rcid + ",";
        str += "]";
        return str;
    }

}
