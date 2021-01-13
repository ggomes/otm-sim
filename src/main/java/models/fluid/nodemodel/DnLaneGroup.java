package models.fluid.nodemodel;

import core.State;
import core.AbstractLaneGroup;

import java.util.HashMap;
import java.util.Map;

public class DnLaneGroup {

    public class StateInfo {
        public double delta_hs;
        public double f_hs;
        public void reset(){
            delta_hs = Double.NaN;
            f_hs = 0d;
        }
    }

    public AbstractLaneGroup lg;
    public Map<Long, RoadConnection> rcs;                  // incoming road connections

    public double s_h;                                    // supply
    public boolean is_blocked;                            // sh==0
    public Double gamma_h;
    public Map<State,StateInfo> state_infos;

    ////////////////////////////////////////////
    // construction
    ////////////////////////////////////////////

    public DnLaneGroup(AbstractLaneGroup lg){
        this.lg = lg;
        this.state_infos = new HashMap<>();
        this.rcs = new HashMap<>();
    }

    public void add_road_connection(RoadConnection rc){
        rcs.put(rc.id,rc);
    }

    public void add_state(State state){
        state_infos.put(state,new StateInfo());
    }

    ////////////////////////////////////////////
    // update
    ////////////////////////////////////////////

    public void reset(){
        this.is_blocked = false;
        this.s_h = lg.get_long_supply();

//        This should be multiplied by lg.() -> wnorm
//                Problem is if dnLaneGroup is a different model,
//                it does not have wnorm, so one must normalize each time.
//                This is ok.


        this.gamma_h = Double.NaN;
        state_infos.values().forEach(x->x.reset());
    }

    public void update_is_blocked(){
        if(!is_blocked)
            is_blocked = s_h < NodeModel.eps;
    }

}
