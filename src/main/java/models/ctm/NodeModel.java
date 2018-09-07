/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.ctm;

import common.AbstractLaneGroup;
import common.Link;
import common.Node;
import error.OTMErrorLog;
import error.OTMException;
import keys.KeyCommPathOrLink;
import runner.Scenario;

import java.util.*;

public class NodeModel {

    private static int MAX_ITERATIONS = 10;
    public static double eps = 1e-3;

    public Node node;

    public Map<Long,UpLaneGroup> ulgs;  // upstrm lane groups.
    public Map<Long,RoadConnection> rcs;  // road connections.
    public Set<DnLink> dlks;  // dnstrm links.

    public NodeModel(Node node) throws OTMException {

        this.node = node;
        rcs = new HashMap<>();

        // if the node has no road connections, it is because it is a source node, a sink node,
        // or a many-to-one node. NodeModels are not constructed for sources and sinks, so it
        // must be many-to-one.
        if(node.road_connections.isEmpty()){

            assert(!node.is_sink && !node.is_source);
            assert(node.is_many2one);
            assert(node.in_links.size()==1);

            // TODO: GENERALIZE THIS FOR MANY-TO-ONE

            Link up_link = node.in_links.values().iterator().next();
            Link dn_link = node.out_links.values().iterator().next();

            // add a fictitious road connection with id 0
            RoadConnection rc = new RoadConnection(0L,new HashSet(dn_link.lanegroups.values()),null);
            rcs.put(0L,rc);

            // there is only one upstream link and one upstream lanegroup
            assert(up_link.lanegroups.size()==1);
            AbstractLaneGroup up_lanegroup = up_link.lanegroups.values().iterator().next();
            ulgs = new HashMap<>();
            UpLaneGroup ulg = new UpLaneGroup((models.ctm.LaneGroup) up_lanegroup);
            ulgs.put(ulg.lg.id,ulg);

            rc.add_up_lanegroup(ulg);
            ulg.add_road_connection(rc);

            // there is only one downstream link but it may have multiple lanegroups
            dlks = new HashSet<>();
            DnLink dlink = new DnLink(dn_link);
            dlks.add(dlink);
            rc.dn_link = dlink;
            dlink.add_road_connection(rc);

            return;
        }

        // case: complete road connections (could be many-to-one) ............................................

        Map<Long,UpLaneGroup> up_lgs_map = new HashMap<>();
        Map<Long,DnLink> dn_links_map = new HashMap<>();

        // iterate through the road connections
        for (common.RoadConnection xrc : node.road_connections) {

            // skip road connections starting or ending in non-macro links
            if( xrc.start_link.model_type!=Link.ModelType.mn && xrc.start_link.model_type!=Link.ModelType.ctm )
                continue;

            RoadConnection rc = new RoadConnection(xrc.getId(),xrc.out_lanegroups,xrc);
            rcs.put(xrc.getId(),rc);

            // go through its upstream lanegroups
            for (AbstractLaneGroup xup_lg : xrc.in_lanegroups) {

                UpLaneGroup ulg;
                if (!up_lgs_map.containsKey(xup_lg.id)) {
                    ulg = new UpLaneGroup((models.ctm.LaneGroup) xup_lg);
                    up_lgs_map.put(xup_lg.id, ulg);
                } else
                    ulg = up_lgs_map.get(xup_lg.id);
                rc.add_up_lanegroup(ulg);
                ulg.add_road_connection(rc);
            }

            // add its end link
            DnLink dn_link;
            if (!dn_links_map.containsKey(xrc.end_link.getId())) {
                dn_link = new DnLink(xrc.end_link);
                dn_links_map.put(xrc.end_link.getId(), dn_link);
            } else
                dn_link = dn_links_map.get(xrc.end_link.getId());
            rc.dn_link = dn_link;
            dn_link.add_road_connection(rc);
        }

        ulgs = up_lgs_map;
        dlks = new HashSet<>(dn_links_map.values());
    }

    public void validate(OTMErrorLog errorLog) {

    }

    public void initialize(Scenario scenario) {

        // allocate states in ulgs
        ulgs.values().forEach( ulg -> ulg.lg.states.forEach( state -> ulg.add_state(state)));
    }

    public void update_flow(float timestamp,boolean is_sink) {

        // TODO HOW TO SKIP LANEGROUPS WITH NO STATE

        // iterate uplgs
        for (UpLaneGroup ulg : ulgs.values()) {

            ulg.reset();

            // demands per state and road connection
            for (KeyCommPathOrLink state : ulg.lg.states) {

                // get the d_icp from the lane group
                Double d_icp = ulg.lg.get_demand_in_target_for_comm_pathORlink(state);

                // copy demands to the ulg
                ulg.d_icp.put(state, d_icp);

                // aggregate to demands per road connection
                Long rc_id = ulg.lg.state2roadconnection.get(state);
                if(rc_id!=null)
                    ulg.d_ir.put(rc_id, ulg.d_ir.get(rc_id) + d_icp);
            }

            // compute state proportions
            for (KeyCommPathOrLink state : ulg.lg.states) {
                Long rc_id = ulg.lg.state2roadconnection.get(state);
                if(rc_id!=null) {
                    Double d_ir = ulg.d_ir.get(rc_id);
                    ulg.eta_icp.put(state, d_ir > 0d ? ulg.d_icp.get(state) / d_ir : 0d);
                }
            }

        }

        // reset road connections
        rcs.values().forEach(rc->rc.reset(node.network.scenario.sim_dt));

        // iterate dlgs
        for (DnLink dlk : dlks) {
            dlk.reset();

            // copy supply to dlgs
            dlk.s_j = dlk.link.model.get_supply();
        }

        // iteration
        int it = 0;
        while (it++ < MAX_ITERATIONS) {
            step0();
            if (eval_stop())
                break;
            step1();
            step2();
            step3();
            step4();
            step5();
            step6();
            step7();
        }

        // update flow accumulators
        // TODO CHECK THIS
//        update_flow_accumulators();
    }

    private boolean eval_stop(){
        // stop if all ulgs lanegroups are either empty or blocked
        return ulgs.values().stream().allMatch(ulg->ulg.is_empty_or_blocked);
    }

    private void step0(){
        /**
         * dlks.is_blocked
         * rcs.is_blocked
         * uplgs.is_empty_or_blocked
         */

        // block downstream lanegroups with zero supply
        dlks.forEach(dlk -> dlk.update_is_blocked());

        // block road connections connecting to blocked links or with control rate = 0
        rcs.values().forEach(rc->rc.update_is_blocked());

        // upstream lanegroup is empty if sum of demands is zero
        // and blocked if any of it connectors is blocked
        ulgs.values().forEach(ulg->ulg.update_is_empty_or_blocked());
    }

    private void step1(){
        /**
         * rcs.d_r
         * rcs.alpha_rj
         */

        for(RoadConnection rc : rcs.values()){

            // Question. Can this be moved to the beginning? Included in the
            // definition of rc.is_blocked? Would this lead to an iteration?
            if(rc.ulgs.isEmpty() || rc.dn_link==null) {
                rc.d_r = 0d;
                continue;
            }

            // total d_r on this lanegroup
            rc.d_r = rc.ulgs.stream().mapToDouble(ulg->ulg.d_ir.get(rc.id)).sum();
        }
    }

    private void step2(){
        /**
         * dlgs.gamma_j
         */

        for(DnLink dlk : dlks ) {

            if(dlk.rcs.isEmpty()) {
                dlk.gamma_j = 1d;
                continue;
            }

            if(Double.isInfinite(dlk.s_j)) {
                dlk.gamma_j = 1d;
                continue;
            }

            if(dlk.is_blocked){
                dlk.gamma_j = Double.POSITIVE_INFINITY;
                continue;
            }

            // total_demand = sum of demands in upstream road connections, times the proportion
            // directed at this lanegroup
            Double d_j = dlk.rcs.stream().mapToDouble(rc-> Math.min(rc.d_r,rc.fbar)).sum();

            // gamma is the d_r-supply ratio
            dlk.gamma_j = Math.max( 1d , d_j / dlk.s_j );
        }
    }

    private void step3(){
        for(RoadConnection rc : rcs.values() )
            rc.gamma_r = rc.is_blocked ? Double.POSITIVE_INFINITY : Math.max( rc.dn_link.gamma_j , rc.d_r / rc.fbar );
    }

    private void step4(){
        /**
         * ulgs.gamma_i
         */

        for(UpLaneGroup ulg : ulgs.values() )
            ulg.gamma_i = ulg.rcs.values().stream()
                    .mapToDouble(rc -> rc.gamma_r)
                    .max().getAsDouble();
    }

    private void step5(){
        /**
         * ulgs.delta_ir
         * ulgs.d_ir
         * ulgs.delta_icp
         * ulgs.f_icp
         */

        for(UpLaneGroup ulg : ulgs.values()){
            for(RoadConnection rc : ulg.rcs.values()) {

                // compute delta_ir
                Double delta_ir = ulg.d_ir.get(rc.id) / ulg.gamma_i;

                // discount from demand
                ulg.d_ir.put(rc.id, ulg.d_ir.get(rc.id) - delta_ir );

                Set<KeyCommPathOrLink> states = ulg.lg.roadconnection2states.get(rc.id);

                if(states!=null)
                    for(KeyCommPathOrLink state : states ) {

                        // compute delta_icp for all states using this road connection
                        Double delta_icp = delta_ir * ulg.eta_icp.get(state);
                        ulg.delta_icp.put( state, delta_icp );

                        // accumulate f_icp
                        ulg.f_icp.put(state, ulg.f_icp.get(state) + delta_icp);
                    }

            }
        }
    }

    private void step6(){
        /**
         * rcs.delta_rcp
         * rcs.f_rcp
         */

        for(RoadConnection rc : rcs.values()){
            // iterate through states that use this road connection
            for(KeyCommPathOrLink state : rc.delta_rcp.keySet() ){
                Double delta_rcp = 0d;
                for( UpLaneGroup ulg : rc.ulgs)
                    if(ulg.delta_icp.containsKey(state))
                        delta_rcp += ulg.delta_icp.get(state);

                // accumulate state flow
                rc.delta_rcp.put(state,delta_rcp);

                if(delta_rcp>0)
                    rc.f_rcp.put(state, rc.f_rcp.get(state) + delta_rcp);
            }
        }
    }

    private void step7(){
        /**
         * dlgs.s_j
         */
        for(RoadConnection rc : rcs.values())
            for( Double delta_rcp : rc.delta_rcp.values() )
                rc.dn_link.s_j -= delta_rcp;
    }

    private void update_flow_accumulators(){

//        for(UpLaneGroup ulg : ulgs){
//
//            if(ulg.lg.commodity_flow_accumulators==null)
//                continue;
//
//            Long lg_id = ulg.lg.id;
//
//            // iterate through the flow accumulators for this lanegroup
//            for(Map.Entry<Long,FlowAccumulator> e : ulg.lg.commodity_flow_accumulators.entrySet()) {
//
//                // compute flow for this commodity
//                Long comm_id = e.getKey();
//
//                double flow_comm = in_flows.containsKey(lg_id) ?
//                        in_flows.get(lg_id).entrySet().stream()
//                                .filter(k -> k.getKey().commodity_id == comm_id)
//                                .mapToDouble(k -> k.getValue())
//                                .sum()
//                        : 0d;
//
//                // increment the accumulators
//                FlowAccumulator fac = e.getValue();
//                fac.increment(flow_comm);
//                if (ulg.lg.global_flow_accumulator != null)
//                    ulg.lg.global_flow_accumulator.increment(flow_comm);
//            }
//
//        }
//
//        // update flow accumulators
//        for(UpLaneGroup ulg : ulgs){
//            if(ulg.lg.commodity_flow_accumulators==null)
//                continue;
//            for(Map.Entry<Long,FlowAccumulator> e : ulg.lg.commodity_flow_accumulators.entrySet()){
//                Long comm_id = e.getKey();
//                double flow = ulg.get_flow_for_commodity(comm_id);
//                FlowAccumulator fac = e.getValue();
//                fac.increment(flow);
//                if(ulg.lg.global_flow_accumulator!=null)
//                    ulg.lg.global_flow_accumulator.increment(flow);
//            }
//        }

    }

}
