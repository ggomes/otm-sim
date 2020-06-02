package common;

import keys.State;

import java.util.HashMap;
import java.util.Map;

public class FlowAccumulatorState {

    public Map<State,Double> count = new HashMap<>();     // key -> count

    public void reset(){
        count = new HashMap<>();
    }

    public void add_state(State state){
        if(!count.containsKey(state))
            count.put(state,0d);
    }

    public void increment(State key, Double x){
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
