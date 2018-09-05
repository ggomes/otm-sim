package models.ctm;

import common.AbstractLaneGroup;
import keys.KeyCommPathOrLink;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RoadConnection {

    public long id;
    public common.RoadConnection rc;
    public Set<UpLaneGroup> ulgs;
    public DnLink dn_link;
    public Set<AbstractLaneGroup> arrive_lanegroups;

    public double fbar; // vps, imposed by external controller;

    public boolean is_blocked;
    public Double d_r;
    public Double gamma_r;

    // state:
    // pathfull, commid, pathid
    // pathless, commid, node.outlink id
    public Map<KeyCommPathOrLink,Double> f_rcp;    // state -> flow

    public Map<KeyCommPathOrLink,Double> delta_rcp; // state -> flow bit

    public RoadConnection(Long id, Set<AbstractLaneGroup> arrive_lanegroups, common.RoadConnection rc){
        this.id = id;
        this.arrive_lanegroups = arrive_lanegroups;
        this.rc = rc;
        this.ulgs = new HashSet<>();
        this.d_r = 0d;
        this.delta_rcp = new HashMap<>();
        this.f_rcp = new HashMap<>();
        this.dn_link = null;
    }

    public void add_up_lanegroup(UpLaneGroup x){
        ulgs.add(x);
    }

    public void add_state(KeyCommPathOrLink state){
        delta_rcp.put(state,0d);
        f_rcp.put(state,0d);
    }

    public void reset(float sim_dt){
        is_blocked = false;
        f_rcp.keySet().forEach(key->f_rcp.put(key,0d));

        if(Double.isInfinite(rc.external_max_flow_vps))
            fbar = Double.POSITIVE_INFINITY;
        else if(rc.external_max_flow_vps<NodeModel.eps)
            fbar = 0d;
        else
            fbar = rc.external_max_flow_vps * sim_dt;
    }

    public void update_is_blocked(){
        if(!is_blocked)
            is_blocked = dn_link.is_blocked || rc.external_max_flow_vps <NodeModel.eps;
    }

    @Override
    public String toString() {
        String str = "";
        str += String.format("rc: link %d, ",id);

        str += "ulgs=[";
        for(UpLaneGroup ulg : ulgs)
            str += ulg.lg.id + ",";
        str += "] ";

        return str;
    }
}
