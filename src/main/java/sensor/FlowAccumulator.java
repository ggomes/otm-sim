package sensor;

import keys.KeyCommPathOrLink;

import java.util.HashMap;
import java.util.Map;

public class FlowAccumulator {

    public Map<KeyCommPathOrLink,Double> count = new HashMap<>();     // key -> count

    public void reset(){
        count = new HashMap<>();
    }

    public void add_key(KeyCommPathOrLink key){
        if(!count.containsKey(key))
            count.put(key,0d);
    }

    public void increment(KeyCommPathOrLink key, Double x){
        if(x.isNaN())
            return;
        count.put(key,x + (count.containsKey(key)?count.get(key):0d));
    }

    public double get_total_count(){
        return count.values().stream().mapToDouble(x->x).sum();
    }

    public double get_count_for_commodity(Long comm_id){
        return count.entrySet().stream()
                .filter(x->x.getKey().commodity_id==comm_id)
                .mapToDouble(x->x.getValue())
                .sum();
    }

}
