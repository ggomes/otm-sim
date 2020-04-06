package models.fluid.nodemodel;

import common.Link;
import common.Node;
import error.OTMErrorLog;
import error.OTMException;
import keys.KeyCommPathOrLink;
import models.AbstractLaneGroup;
import models.fluid.*;
import models.fluid.FluidLaneGroup;
import common.Scenario;
import utils.OTMUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class NodeModel {

//    private static boolean verbose = false;
    private static int MAX_ITERATIONS = 10;
    public static double eps = 1e-3;
    public Node node;

    public Map<Long, UpLaneGroup> ulgs;  // upstrm lane groups.
    public Map<Long, RoadConnection> rcs;  // road connections.
    public Map<Long, DnLaneGroup> dlgs; /// dnstrm lane groups.

    public NodeModel(Node node) {
        this.node = node;
    }

    public void build() throws OTMException {

        rcs = new HashMap<>();

        // if the node has no road connections, it is because it is a source node, a sink node,
        // or a many-to-one node. NodeModels are not constructed for sources and sinks, so it
        // must be many-to-one.
        if(node.road_connections.isEmpty()){

            // this should not happen because we are completing road connections
            assert(false);
            assert(!node.is_sink && !node.is_source);
            assert(node.is_many2one);

            // currently works only for one-to-one
            // TODO: GENERALIZE THIS FOR MANY-TO-ONE

            // there is only one upstream link
            assert(node.in_links.size()==1);
            Link up_link = node.in_links.values().iterator().next();

            // there is only one dnstream link
            assert(node.out_links.size()==1);
            Link dn_link = node.out_links.values().iterator().next();

            // there is only one upstream lanegroup
            assert(up_link.lanegroups_flwdn.size()==1);
            FluidLaneGroup up_lanegroup = (FluidLaneGroup) up_link.lanegroups_flwdn.values().iterator().next();

            // there is only one dnstream lanegroup
            assert(dn_link.lanegroups_flwdn.size()==1);
            FluidLaneGroup dn_lanegroup = (FluidLaneGroup) dn_link.lanegroups_flwdn.values().iterator().next();

            // add a fictitious road connection with id 0
            RoadConnection rc = new RoadConnection(0L,null);
            rcs.put(0L,rc);

            // ulgs
            ulgs = new HashMap<>();
            UpLaneGroup ulg = new UpLaneGroup(up_lanegroup);
            ulgs.put(up_lanegroup.id,ulg);
            ulg.add_road_connection(rc);
            rc.add_up_lanegroup(ulg);

            // dlgs
            dlgs = new HashMap<>();
            DnLaneGroup dlg = new DnLaneGroup(dn_lanegroup);
            dlgs.put(dn_lanegroup.id,dlg);
            rc.add_dn_lanegroup(dlg);
            dlg.add_road_connection(rc);

            return;
        }

        // case: complete road connections (could be many-to-one) ............................................

        Map<Long,UpLaneGroup> up_lgs_map = new HashMap<>();
        Map<Long,DnLaneGroup> dn_lgs_map = new HashMap<>();

        // iterate through the road connections
        for (common.RoadConnection xrc : node.road_connections) {

            // skip road connections starting in discrete event links
            if( xrc.get_start_link()==null )
                continue;

            // skip if it is disconnected
            if( xrc.in_lanegroups.isEmpty() || xrc.out_lanegroups.isEmpty())
                continue;

            // incoming fluid lane groups
            Set<AbstractLaneGroup> in_fluid_lgs = xrc.in_lanegroups.stream()
                    .filter(x -> x.link.model instanceof AbstractFluidModel)
                    .collect(toSet());

            if( in_fluid_lgs.isEmpty() )
                continue;

            RoadConnection rc = new RoadConnection(xrc.getId(),xrc);
            rcs.put(xrc.getId(),rc);

            // go through its upstream lanegroups
            for (AbstractLaneGroup xup_lg : in_fluid_lgs) {

                UpLaneGroup ulg;
                if (!up_lgs_map.containsKey(xup_lg.id)) {
                    ulg = new UpLaneGroup((FluidLaneGroup) xup_lg);
                    up_lgs_map.put(xup_lg.id, ulg);
                } else
                    ulg = up_lgs_map.get(xup_lg.id);
                ulg.add_road_connection(rc);
                rc.add_up_lanegroup(ulg);

            }

            // go through its downstream lanegroups
            for (AbstractLaneGroup xdn_lg : xrc.out_lanegroups) {
                DnLaneGroup dlg;
                if (!dn_lgs_map.containsKey(xdn_lg.id)) {
                    dlg = new DnLaneGroup(xdn_lg);
                    dn_lgs_map.put(xdn_lg.id, dlg);
                } else
                    dlg = dn_lgs_map.get(xdn_lg.id);
                rc.add_dn_lanegroup(dlg);
                dlg.add_road_connection(rc);
            }

        }
        ulgs = up_lgs_map;
        dlgs = dn_lgs_map;
    }

    public void validate(OTMErrorLog errorLog) {
    }

    public void initialize(Scenario scenario) {
        // allocate states in ulgs
        ulgs.values().forEach( ulg -> ulg.lg.states.forEach( state -> ulg.add_state(state)));
    }

    public Set<KeyCommPathOrLink> get_states_for_road_connection(long rc_id){
        return rcs.containsKey(rc_id) ? rcs.get(rc_id).get_states() : null;
    }

    public void update_flow(float timestamp) {

//        verbose = timestamp==162f && node.getId()==8L;
//
//        if(verbose)
//            System.out.println(timestamp + "\t" + node.getId());

        // reset
        ulgs.values().forEach(x->x.reset());
        rcs.values().forEach(x->x.reset());
        dlgs.values().forEach(x->x.reset());


        // iteration
        int it = 0;
        while (it++ <= MAX_ITERATIONS) {

//            if(verbose){
//                System.out.println("\t------- "+it+" ----------------");
//]
//                System.out.println("\tulgs:");
//                ulgs.values().forEach(ulg->System.out.println(String.format("\t\tlg=%d\td=%f",
//                        ulg.lg.id,
//                        ulg.state_infos.values().stream().mapToDouble(x->x.d_gs).sum())));
//
//                System.out.println("\tdlgs:");
//                dlgs.values().forEach(dlg->System.out.println(String.format("\t\tlg=%d\ts=%f",
//                        dlg.lg.id,
//                        dlg.s_h)));
//            }

            step0();


//            if(verbose){
//                System.out.println("\tstep0 ulgs:");
//                ulgs.values().forEach(ulg->System.out.println(String.format("\t\t%d is empty or blocked=%s",
//                        ulg.lg.link.getId(),
//                        ulg.is_empty_or_blocked)));
//
//                System.out.println("\tstep0 rcs:");
//                rcs.values().forEach(rc->System.out.println(String.format("\t\t%d is blocked=%s",
//                        rc.id,rc.is_blocked)));
//
//                System.out.println("\tstep0 dlgs:");
//                dlgs.values().forEach(dlg->System.out.println(String.format("\t\t%d is blocked = %s",
//                        dlg.lg.link.getId(),
//                        dlg.is_blocked)));
//            }

            if (eval_stop(it))
                break;

            step1();

//            if(verbose){
//                System.out.println("\tstep1 rcs:");
//                System.out.println(String.format("\t\t%d dr=%f",106,rcs.get(106L).d_r));
//                System.out.println(String.format("\t\t%d dr=%f",111,rcs.get(111L).d_r));
//                System.out.println(String.format("\t\t%d dr=%f",112,rcs.get(112L).d_r));
//                System.out.println(String.format("\t\t%d dr=%f",113,rcs.get(113L).d_r));
//            }

            step2();

//            if(verbose){
//                System.out.println("\tstep2 dlgs:");
//                System.out.println(String.format("\t\t%d gh=%f",115,dlgs.get(115L).gamma_h));
//                System.out.println(String.format("\t\t%d gh=%f",24,dlgs.get(24L).gamma_h));
//                System.out.println(String.format("\t\t%d gh=%f",26,dlgs.get(26L).gamma_h));
//                System.out.println(String.format("\t\t%d gh=%f",27,dlgs.get(27L).gamma_h));
//            }

            step3();
            step4();
            step5();
            step6();
        }

        // update flow accumulators
        // TODO CHECK THIS
//        update_flow_accumulators();
    }

    private boolean eval_stop(int iteration){

        if(iteration>MAX_ITERATIONS){
            System.out.println("Reached iteration limit for node " + node.getId());
            return true;
        }

        // stop if all ulgs lanegroups are either empty or blocked
        return ulgs.values().stream().allMatch(ulg->ulg.is_empty_or_blocked);
    }

    private void step0(){
        /**
         * dlgs.is_blocked
         * rcs.is_blocked
         * uplgs.is_empty_or_blocked
         */

        // block downstream lanegroups with zero supply
        dlgs.values().forEach(dlg -> dlg.update_is_blocked());

        // block road connections connecting to blocked links or with control rate = 0
        rcs.values().forEach(rc->rc.update_is_blocked());

        // upstream lanegroup is empty if sum of demands is zero
        // and blocked if any of it connectors is blocked
        ulgs.values().forEach(ulg->ulg.update_is_empty_or_blocked());

    }

    private void step1(){
        /** d_r, alpha_rh
         */

        for(RoadConnection rc : rcs.values()) {

            rc.d_r = rc.is_blocked ? 0d : rc.ulgs.stream()
                    .filter(ulg->!ulg.is_empty_or_blocked)
                    .mapToDouble(ulg -> ulg.rc_infos.get(rc.id).d_gr)
                    .sum();

            if(rc.d_r< NodeModel.eps)
                continue;

            boolean any_is_infinite = rc.dnlg_infos.values().stream().anyMatch(x-> Double.isInfinite(x.dlg.s_h));

            if(any_is_infinite){

                // distribute equally
                double p = 1d/rc.dnlg_infos.size();

                // alpha_rh: distribution amongst downstream lanegroups
                rc.dnlg_infos.values().forEach( x -> x.alpha_rh =  x.lambda_rh * p );


            } else {
                // s_r: downstream supply seen by this road connection
                double s_r = rc.dnlg_infos.values().stream()
                        .mapToDouble(x->x.lambda_rh *x.dlg.s_h).sum();

                // alpha_rh: distribution amongst downstream lanegroups
                rc.dnlg_infos.values().forEach( x -> x.alpha_rh = s_r<OTMUtils.epsilon ? 0d : x.lambda_rh * x.dlg.s_h / s_r);
            }
        }

    }

    private void step2(){
        /**  gamma_h */

        for(DnLaneGroup dlg : dlgs.values() ) {

            // for MN model
            if(Double.isInfinite(dlg.s_h))
                dlg.gamma_h = 0d;

            else if (dlg.is_blocked)
                dlg.gamma_h = 1d;

            else {

                // total_demand = sum of demands in upstream road connections, times the proportion
                // directed at this lanegroup
                Double d_h = dlg.rcs.values().stream().mapToDouble(rc -> rc.dnlg_infos.get(dlg.lg.id).alpha_rh * Math.min(rc.d_r, rc.fbar)).sum();
                dlg.gamma_h = d_h>dlg.s_h ? 1d-dlg.s_h /d_h : 0d;
            }

        }
    }

    private void step3(){
        /** gamma_r */
        for(RoadConnection rc : rcs.values() )
            rc.gamma_r = rc.is_blocked ? 0d : rc.dnlg_infos.values().stream().mapToDouble(x -> x.dlg.gamma_h * x.alpha_rh).sum();
    }

    private void step4(){
        /** gamma_g, delta_gs, f_gs */

        for(UpLaneGroup ulg : ulgs.values() ) {
            ulg.gamma_g = ulg.is_empty_or_blocked ?
                    1d :
                    ulg.rc_infos.values().stream().mapToDouble(rc -> rc.rc.gamma_r).max().getAsDouble();

            if(!ulg.is_empty_or_blocked){

                for( Map.Entry<KeyCommPathOrLink,UpLaneGroup.StateInfo> e : ulg.state_infos.entrySet()){

                    KeyCommPathOrLink state = e.getKey();
                    UpLaneGroup.StateInfo stateInfo = e.getValue();

                    stateInfo.delta_gs = stateInfo.d_gs * (1d-ulg.gamma_g);
                    stateInfo.d_gs -= stateInfo.delta_gs;
                    ulg.f_gs.put(state,ulg.f_gs.get(state)+stateInfo.delta_gs);

                    // reduce d_gr
                    if(ulg.lg.state2roadconnection.containsKey(state) ){
                        Long rc_id = ulg.lg.state2roadconnection.get(state);
                        if(ulg.rc_infos.containsKey(rc_id))
                            ulg.rc_infos.get(rc_id).d_gr -= stateInfo.delta_gs;
                    }
                }
            }

        }

    }

    private void step5(){
        /** delta_rs, f_rs */

        for(RoadConnection rc : rcs.values()){

            rc.delta_r = 0d;

            for(KeyCommPathOrLink state : rc.f_rs.keySet()){

                // delta_rs, f_rs
                double delta_rs = rc.ulgs.stream()
                        .filter(x->!x.is_empty_or_blocked)
                        .mapToDouble(x->x.state_infos.get(state).delta_gs)
                        .sum();

                rc.delta_r += delta_rs;
                rc.f_rs.put( state , rc.f_rs.get(state) + delta_rs );

            }
        }
    }

    private void step6(){
        /** s_h */

        for(DnLaneGroup dlg : dlgs.values() ) {
            double sum = dlg.rcs.values().stream()
                    .mapToDouble(rc -> rc.delta_r*rc.dnlg_infos.get(dlg.lg.id).alpha_rh/(1d-rc.gamma_r))
                    .sum();
            dlg.s_h -= (1d-dlg.gamma_h)*sum;
        }

    }



//    private void update_flow_accumulators(){
//        for(UpLaneGroup ulg : ulgs){
//
//            if(ulg.lg.commodity_flow_accumulators==null)
//                continue;
//
//            Long lg_id = ulg.lg.id;
//
//            // iterate through the flow accumulators for this lanegroup
//            for(Map.Entry<Long,FlowAccumulatorState> e : ulg.lg.commodity_flow_accumulators.entrySet()) {
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
//                FlowAccumulatorState fac = e.getValue();
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
//            for(Map.Entry<Long,FlowAccumulatorState> e : ulg.lg.commodity_flow_accumulators.entrySet()){
//                Long comm_id = e.getKey();
//                double flow = ulg.get_flow_for_commodity(comm_id);
//                FlowAccumulatorState fac = e.getValue();
//                fac.increment(flow);
//                if(ulg.lg.global_flow_accumulator!=null)
//                    ulg.lg.global_flow_accumulator.increment(flow);
//            }
//        }
//    }

}
