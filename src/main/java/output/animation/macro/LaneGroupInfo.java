/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package output.animation.macro;

import common.AbstractLaneGroup;
import keys.KeyCommPathOrLink;
import models.ctm.LaneGroup;
import output.animation.AbstractLaneGroupInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LaneGroupInfo extends AbstractLaneGroupInfo {

    public List<CellInfo> cell_info;

    //////////////////////////////////////////////////
    // contsruction
    //////////////////////////////////////////////////

    public LaneGroupInfo(AbstractLaneGroup lg) {
        super(lg);

        LaneGroup ctm_lg = (LaneGroup) lg;
        this.cell_info = new ArrayList<>();
        for(int i=0;i<ctm_lg.cells.size();i++)
            cell_info.add(new CellInfo(ctm_lg.cells.get(i),i));
    }

    //////////////////////////////////////////////////
    // get
    //////////////////////////////////////////////////

    public int get_num_cells(){
        return cell_info.size();
    }

    public Map<KeyCommPathOrLink,Double> get_vehicles_by_cell(int i){
        return cell_info.get(i).comm_vehicles;
    }

    public List<Double> get_total_vehicles_by_cell(){
        return cell_info.stream().map(x->x.get_total_vehicles()).collect(Collectors.toList());
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
