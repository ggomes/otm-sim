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
        if(cell.veh_dwn !=null)
            keySet.addAll(cell.veh_dwn.keySet());
        if(cell.veh_out !=null)
            keySet.addAll(cell.veh_out.keySet());

        comm_vehicles = new HashMap<>();
        for(KeyCommPathOrLink key : keySet){
            double val = 0d;
            if(cell.veh_dwn !=null && cell.veh_dwn.containsKey(key))
                val += cell.veh_dwn.get(key);
            if(cell.veh_out !=null && cell.veh_out.containsKey(key))
                val += cell.veh_out.get(key);
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
