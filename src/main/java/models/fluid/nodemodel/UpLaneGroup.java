package models.fluid.nodemodel;

import common.State;
import models.fluid.FluidLaneGroup;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UpLaneGroup {

    public class RcInfo {
        public final RoadConnection rc;
        public Set<State> S_gr = new HashSet<>();
        public double d_gr;
        public RcInfo(RoadConnection rc) {
            this.rc = rc;
        }
        public void reset(){
            d_gr = S_gr.stream().mapToDouble(state->state_infos.get(state).d_gs).sum();
        }
        public void add_state(State state){
            S_gr.add(state);
            rc.add_state(state);
        }
    }

    public class StateInfo {
        public final State state;
        public double d_gs;
        public double delta_gs;
        public StateInfo(State state){
            this.state = state;
        }
        public void reset(){
            d_gs = lg.get_demand().get(state);
            delta_gs = Double.NaN;
        }
    }

    public FluidLaneGroup lg;

    public boolean is_empty_or_blocked;
    public double gamma_g;
    public Map<State,StateInfo> state_infos;
    public Map<State,Double> f_gs;
    public Map<Long,RcInfo> rc_infos;

    ////////////////////////////////////////////
    // construction
    ////////////////////////////////////////////

    public UpLaneGroup(FluidLaneGroup lg){
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

    public void add_state(State state){
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
