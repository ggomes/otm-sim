/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package output.animation.macro;

import keys.KeyCommPathOrLink;
import models.ctm.Cell;

import java.util.*;

public class CellInfo {

    public int index;
    public Map<KeyCommPathOrLink,Double> comm_vehicles;  // commodity->vehicles

    //////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////

    public CellInfo(Cell cell,int index){
        this.index = index;

        Set<KeyCommPathOrLink> keySet = new HashSet<>();
        if(cell.veh_in_target!=null)
            keySet.addAll(cell.veh_in_target.keySet());
        if(cell.veh_notin_target!=null)
            keySet.addAll(cell.veh_notin_target.keySet());

        comm_vehicles = new HashMap<>();
        for(KeyCommPathOrLink key : keySet){
            double val = 0d;
            if(cell.veh_in_target!=null && cell.veh_in_target.containsKey(key))
                val += cell.veh_in_target.get(key);
            if(cell.veh_notin_target!=null && cell.veh_notin_target.containsKey(key))
                val += cell.veh_notin_target.get(key);
            comm_vehicles.put(key,val);
        }
    }

    //////////////////////////////////////////////////
    // get
    //////////////////////////////////////////////////

    public Double get_total_vehicles(){
        return comm_vehicles.values().stream().reduce(0d,(i,j)->i+j);
    }

    @Override
    public String toString() {

        if(this.comm_vehicles.keySet().size()>1) {
            String str = "\t\t\tcell " + index + "\n";
            for(Map.Entry<KeyCommPathOrLink,Double> e : comm_vehicles.entrySet())
                str += "\t\t\t\t" + "comm " + e.getKey()  +"\t" + e.getValue() + "\n";
            return str;
        } else {
            return String.format("%.1f, ",get_total_vehicles());
        }
    }
}
