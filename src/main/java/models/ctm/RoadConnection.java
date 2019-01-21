/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.ctm;

import keys.KeyCommPathOrLink;
import models.MacroNodeModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RoadConnection {

    public class DnLgInfo {
        public final DnLaneGroup dlg;
        public final double lambda_rj;
        public double alpha_rj;
        public DnLgInfo(DnLaneGroup dlg,double lambda_rj){
            this.dlg = dlg;
            this.lambda_rj = lambda_rj;
        }
        public void reset(){
            alpha_rj = 0f;
        }
    }

    public long id;
    public common.RoadConnection rc;

    public boolean is_blocked;
    public Double d_r;
    public Double gamma_r;
    public double fbar; // vps, imposed by external controller;

    public Set<UpLaneGroup> ulgs;
    public Map<Long,DnLgInfo> dnlg_infos;
    public Map<KeyCommPathOrLink,Double> f_rs;

    ////////////////////////////////////////////
    // construction
    ////////////////////////////////////////////

    public RoadConnection(Long id, common.RoadConnection rc){
        this.id = id;
        this.rc = rc;
        this.d_r = Double.NaN;
        this.gamma_r = Double.NaN;
        this.ulgs = new HashSet<>();
        this.dnlg_infos = new HashMap<>();
        this.f_rs = new HashMap<>();
    }

    public void add_up_lanegroup(UpLaneGroup x){
        this.ulgs.add(x);
    }

    public void add_dn_lanegroup(DnLaneGroup x){

        // compute lambda_rj
        int lg_from_lane = x.lg.start_lane_up;
        int lg_to_lane = x.lg.start_lane_up + x.lg.num_lanes - 1;

        int overlap_from_lane = Math.max(lg_from_lane,rc.end_link_from_lane);
        int overlap_to_lane = Math.min(lg_to_lane,rc.end_link_to_lane);

        double lg_total_lanes = (double) x.lg.num_lanes;
        double rc_covered_lanes = (double) (overlap_to_lane-overlap_from_lane+1);
        double lambda_rj = rc_covered_lanes / lg_total_lanes;

        dnlg_infos.put(x.lg.id,new DnLgInfo(x,lambda_rj));
    }

    public void add_state(KeyCommPathOrLink state){
        f_rs.put(state,0d);

        for(DnLgInfo dnLgInfo : dnlg_infos.values()){
            dnLgInfo.dlg.add_state(state);
        }
    }

    ////////////////////////////////////////////
    // update
    ////////////////////////////////////////////

    public void reset(){
        is_blocked = false;
        d_r = Double.NaN;
        gamma_r = Double.NaN;

        dnlg_infos.values().forEach(x->x.reset());

        f_rs.keySet().forEach(x->f_rs.put(x,0d));

        // fbar
        if(Double.isInfinite(rc.external_max_flow_vps))
            fbar = Double.POSITIVE_INFINITY;
        else if(rc.external_max_flow_vps< MacroNodeModel.eps)
            fbar = 0d;
        else {
            float dt = ((models.ctm.Model_CTM)this.rc.get_start_link().model).dt;
            fbar = rc.external_max_flow_vps * dt;
        }
    }

    public void update_is_blocked(){
        if(!is_blocked)
            is_blocked = dnlg_infos.values().stream().allMatch(x->x.dlg.is_blocked) ||
                         fbar<MacroNodeModel.eps;
    }

}
