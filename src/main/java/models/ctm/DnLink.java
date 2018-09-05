package models.ctm;

import common.Link;

import java.util.HashSet;

public class DnLink {

    public Link link;
    public HashSet<RoadConnection> rcs;

    public boolean is_blocked;
    public double s_j;
    public double gamma_j;

    public DnLink(Link link){
        this.link = link;
        this.rcs = new HashSet<>();
        this.is_blocked = false;
        this.s_j = 0d;
        this.gamma_j = 1d;
    }

    public void add_road_connection(RoadConnection x){
        rcs.add(x);
    }

    public void reset(){
        is_blocked = false;
        gamma_j = 1d;
    }

    public void update_is_blocked(){
        if(!is_blocked && Double.isFinite(s_j)){
            is_blocked = s_j<NodeModel.eps;
            if(is_blocked)
                s_j = 0d;
        }
    }

    @Override
    public String toString() {
        String str = "";
        str += String.format("dlink: link %d, ",link.getId());
        str += "rcs=[";
        for(RoadConnection rc : rcs)
            str += rc.id + ",";
        str += "]";
        return str;
    }

}
