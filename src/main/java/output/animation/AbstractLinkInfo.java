/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package output.animation;

import common.AbstractLaneGroup;
import common.Link;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractLinkInfo implements InterfaceLinkInfo {

    public Long link_id;
    public Map<Long,AbstractLaneGroupInfo> lanegroup_info;

    //////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////

    public AbstractLinkInfo(Link link){
        this.link_id = link.getId();
        lanegroup_info = new HashMap<>();
        for(AbstractLaneGroup lg : link.long_lanegroups.values())
            lanegroup_info.put(lg.id, newLaneGroupInfo(lg) );
    }

    //////////////////////////////////////////////////
    // get
    //////////////////////////////////////////////////

    public AbstractLaneGroupInfo get_lanegroup_info(long lg_id){
        return lanegroup_info.containsKey(lg_id) ? lanegroup_info.get(lg_id) : null;
    }

    public Double get_total_vehicles() {
        return lanegroup_info.values().stream()
                .mapToDouble(x->x.get_total_vehicles())
                .sum();
    }

    @Override
    public String toString() {
        String str = "\tlink " + link_id + "\n";
        for(AbstractLaneGroupInfo lg : lanegroup_info.values())
            str += lg + "\n";
        return str;
    }

}
