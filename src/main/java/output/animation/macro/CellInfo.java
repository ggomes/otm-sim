package output.animation.macro;

import keys.KeyCommPathOrLink;
import models.ctm.Cell;

import java.util.HashMap;
import java.util.Map;

public class CellInfo {

    public int index;
    public HashMap<Long,Double> comm_vehicles;  // commodity->vehicles

    public CellInfo(Cell cell,int index){
        this.index = index;
        comm_vehicles = new HashMap<>();
        for(Map.Entry<KeyCommPathOrLink,Double> e : cell.veh_in_target.entrySet()){
            Long comm_id = e.getKey().commodity_id;
            if(!comm_vehicles.containsKey(comm_id))
                comm_vehicles.put(comm_id,0d);
            comm_vehicles.put(comm_id,comm_vehicles.get(comm_id)+e.getValue());
        }
    }

    public Double get_total_vehicles(){
        return comm_vehicles.values().stream().reduce(0d,(i,j)->i+j);
    }

    @Override
    public String toString() {
        String str = "\t\t\tcell " + index + "\n";
        for(Map.Entry<Long,Double> e : comm_vehicles.entrySet())
            str += "\t\t\t\t" + "comm " + e.getKey()  +"\t" + e.getValue() + "\n";
        return str;
    }
}
