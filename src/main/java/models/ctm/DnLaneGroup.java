package models.ctm;

import common.AbstractLaneGroup;
import keys.KeyCommPathOrLink;

import java.util.HashMap;
import java.util.Map;

public class DnLaneGroup {

    public class StateInfo {
        public double delta_js;
        public double f_js;
        public void reset(){
            delta_js = Double.NaN;
            f_js = 0d;
        }
    }

    public AbstractLaneGroup lg;
    public Map<Long,RoadConnection> rcs;                  // incoming road connections

    public double s_j;                                    // supply
    public boolean is_blocked;                            // sj==0
    public Double gamma_j;
    public Map<KeyCommPathOrLink,StateInfo> state_infos;

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

    public void add_state(KeyCommPathOrLink state){
        state_infos.put(state,new StateInfo());
    }

    ////////////////////////////////////////////
    // update
    ////////////////////////////////////////////

    public void reset(){
        this.is_blocked = false;
        this.s_j = lg.get_supply();
        this.gamma_j = Double.NaN;
        state_infos.values().forEach(x->x.reset());
    }

    public void update_is_blocked(){
        if(!is_blocked)
            is_blocked = s_j < NodeModel.eps;
    }

}
