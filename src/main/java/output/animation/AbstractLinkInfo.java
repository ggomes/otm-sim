package output.animation;

import common.AbstractLaneGroup;
import common.Link;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractLinkInfo implements InterfaceLinkInfo {

    public Long link_id;
    public Map<Long,AbstractLaneGroupInfo> lanegroup_info;

    public AbstractLinkInfo(Link link){
        this.link_id = link.getId();
        lanegroup_info = new HashMap<>();
        for(AbstractLaneGroup lg : link.lanegroups.values())
            lanegroup_info.put(lg.id, newLaneGroupInfo(lg) );
    }

    public Double get_total_vehicles() {
        return lanegroup_info.values().stream()
                .map(x->x.get_total_vehicles())
                .reduce(0.0, (i,j) -> i+j);
    }

    @Override
    public String toString() {
        String str = "\tlink " + link_id + "\n";
        for(AbstractLaneGroupInfo lg : lanegroup_info.values())
            str += lg + "\n";
        return str;
    }

}
