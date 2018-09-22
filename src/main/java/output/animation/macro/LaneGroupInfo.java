/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package output.animation.macro;


import common.AbstractLaneGroup;
import output.animation.AbstractLaneGroupInfo;

import java.util.ArrayList;
import java.util.List;

public class LaneGroupInfo extends AbstractLaneGroupInfo {

    public List<CellInfo> cell_info;

    public LaneGroupInfo(AbstractLaneGroup lg) {
        super(lg);

        models.ctm.LaneGroup ctm_lg = (models.ctm.LaneGroup) lg;
        this.cell_info = new ArrayList<>();
        for(int i=0;i<ctm_lg.cells.size();i++)
            cell_info.add(new CellInfo(ctm_lg.cells.get(i),i));
    }

    @Override
    public String toString() {
        String str = "\t\tlanegroup " + lg_id + "\n";
        for(CellInfo c : cell_info) {
            if(c.comm_vehicles.keySet().size()>1)
                str += c + "\n";
            else
                str += c.get_total_vehicles() + ", ";
        }
        return str;
    }

    @Override
    public Double get_total_vehicles() {
        return cell_info.stream()
                .mapToDouble(x->x.get_total_vehicles())
                .sum();
    }

}
