package models.fluid.nodemodel;

import core.*;
import error.OTMException;
import models.fluid.FluidLaneGroup;
import utils.OTMUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class NodeModel {

    private static boolean debug = false;
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
        if(node.get_road_connections().isEmpty()){

            // this should not happen because we are completing road connections
            assert(false);

            // currently works only for one-to-one
            // TODO: GENERALIZE THIS FOR MANY-TO-ONE

            // there is only one upstream link
            assert(node.num_inputs()==1);
            Link up_link = node.get_in_links().iterator().next();

            // there is only one dnstream link
            assert(node.num_outputs()==1);
            Link dn_link = node.get_out_links().iterator().next();

            // there is only one upstream lanegroup
            assert(up_link.get_lgs().size()==1);
            FluidLaneGroup up_lanegroup = (FluidLaneGroup) up_link.get_lgs().iterator().next();

            // there is only one dnstream lanegroup
            assert(dn_link.get_lgs().size()==1);
            FluidLaneGroup dn_lanegroup = (FluidLaneGroup) dn_link.get_lgs().iterator().next();

            // add a fictitious road connection with id 0
            RoadConnection rc = new RoadConnection(0L,null);
            rcs.put(0L,rc);

            // ulgs
            ulgs = new HashMap<>();
            UpLaneGroup ulg = new UpLaneGroup(up_lanegroup);
            ulgs.put(up_lanegroup.getId(),ulg);
            ulg.add_road_connection(rc);
            rc.add_up_lanegroup(ulg);

            // dlgs
            dlgs = new HashMap<>();
            DnLaneGroup dlg = new DnLaneGroup(dn_lanegroup);
            dlgs.put(dn_lanegroup.getId(),dlg);
            rc.add_dn_lanegroup(dlg);
            dlg.add_road_connection(rc);

            return;
        }

        // case: complete road connections (could be many-to-one) ............................................

        Map<Long,UpLaneGroup> up_lgs_map = new HashMap<>();
        Map<Long,DnLaneGroup> dn_lgs_map = new HashMap<>();

        // iterate through the road connections
        for (core.RoadConnection xrc : node.get_road_connections()) {

            // skip road connections starting in discrete event links
            if( xrc.get_start_link()==null )
                continue;

            // skip if it is disconnected
            if( xrc.get_in_lanegroups().isEmpty() || xrc.get_out_lanegroups().isEmpty())
                continue;

            // incoming fluid lane groups
            Set<AbstractLaneGroup> in_fluid_lgs = xrc.get_in_lanegroups().stream()
                    .filter(x -> x.get_link().get_model() instanceof AbstractFluidModel)
                    .collect(toSet());

            if( in_fluid_lgs.isEmpty() )
                continue;

            RoadConnection rc = new RoadConnection(xrc.getId(),xrc);
            rcs.put(xrc.getId(),rc);

            // go through its upstream lanegroups
            for (AbstractLaneGroup xup_lg : in_fluid_lgs) {

                UpLaneGroup ulg;
                if (!up_lgs_map.containsKey(xup_lg.getId())) {
                    ulg = new UpLaneGroup((FluidLaneGroup) xup_lg);
                    up_lgs_map.put(xup_lg.getId(), ulg);
                } else
                    ulg = up_lgs_map.get(xup_lg.getId());
                ulg.add_road_connection(rc);
                rc.add_up_lanegroup(ulg);

            }

            // go through its downstream lanegroups
            for (AbstractLaneGroup xdn_lg : xrc.get_out_lanegroups()) {
                DnLaneGroup dlg;
                if (!dn_lgs_map.containsKey(xdn_lg.getId())) {
                    dlg = new DnLaneGroup(xdn_lg);
                    dn_lgs_map.put(xdn_lg.getId(), dlg);
                } else
                    dlg = dn_lgs_map.get(xdn_lg.getId());
                rc.add_dn_lanegroup(dlg);
                dlg.add_road_connection(rc);
            }

        }
        ulgs = up_lgs_map;
        dlgs = dn_lgs_map;
    }

    public void initialize(Scenario scenario) {
        // allocate states in ulgs
        ulgs.values().forEach( ulg -> ulg.lg.get_link().states.forEach( state -> ulg.add_state(state)));
    }

    public Set<State> get_states_for_road_connection(long rc_id){
        return rcs.containsKey(rc_id) ? rcs.get(rc_id).get_states() : null;
    }

    public void update_flow(float timestamp) {

        // reset
        ulgs.values().forEach(x->x.reset());
        rcs.values().forEach(x->x.reset());
        dlgs.values().forEach(x->x.reset());


        // ----------------------------------------
        if(debug && node.getId()==2l){
            System.out.println(String.format("%.1f node model %d START",timestamp,node.getId()));
            for(UpLaneGroup ulg : ulgs.values()){
                for(Map.Entry<State, UpLaneGroup.StateInfo> e : ulg.state_infos.entrySet()){
                    State state = e.getKey();
                    UpLaneGroup.StateInfo stateinfo = e.getValue();
                    System.out.println(String.format("\tlink=%d\tlane=%d\tstate=(%d,%d)\tdemand=%.1f",
                            ulg.lg.get_link().getId(), ulg.lg.get_start_lane_dn(),
                            state.commodity_id,state.pathOrlink_id,stateinfo.d_gs*720d));
                }
            }

            for(DnLaneGroup dlg : dlgs.values())
                System.out.println(String.format("\tlink=%d\tlane=%d\tsupply=%.1f",
                        dlg.lg.get_link().getId(), dlg.lg.get_start_lane_dn(),
                        dlg.s_h*720d));
        }
        // ---------------------------------------



        // iteration
        int it = 0;
        while (it++ <= MAX_ITERATIONS) {

            step0();

            if (eval_stop(it))
                break;

            step1();
            step2();
            step3();
            step4();
            step5();
            step6();
        }


        // ----------------------------------------
        if(debug && node.getId()==2l){
            for(RoadConnection rc : rcs.values()){
                System.out.println(String.format("\trc %d",rc.rc.getId()));
                for(Map.Entry<State,Double> e : rc.f_rs.entrySet()) {
                    State state = e.getKey();
                    System.out.println(String.format("\t\tstate=(%d,%d)\tflow=%.1f",
                            state.commodity_id,state.pathOrlink_id,e.getValue()*720d));
                }
            }
            System.out.println("...................");
        }
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
//                Double d_h = dlg.rcs.values().stream().mapToDouble(rc -> rc.dnlg_infos.get(dlg.lg.id).alpha_rh * Math.min(rc.d_r, rc.fbar)).sum();
                Double d_h = dlg.rcs.values().stream().mapToDouble(rc -> rc.dnlg_infos.get(dlg.lg.getId()).alpha_rh * rc.d_r).sum();
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

                for( Map.Entry<State,UpLaneGroup.StateInfo> e : ulg.state_infos.entrySet()){

                    State state = e.getKey();
                    UpLaneGroup.StateInfo stateInfo = e.getValue();

                    stateInfo.delta_gs = stateInfo.d_gs * (1d-ulg.gamma_g);
                    stateInfo.d_gs -= stateInfo.delta_gs;
                    ulg.f_gs.put(state,ulg.f_gs.get(state)+stateInfo.delta_gs);

                    // reduce d_gr
                    if(ulg.lg.get_link().states.contains(state) ){
                        Long rc_id = ulg.lg.get_rc_for_state(state);
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

            for(State state : rc.f_rs.keySet()){

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
                    .mapToDouble(rc -> rc.delta_r*rc.dnlg_infos.get(dlg.lg.getId()).alpha_rh/(1d-rc.gamma_r))
                    .sum();
            dlg.s_h -= (1d-dlg.gamma_h)*sum;
        }

    }

}
