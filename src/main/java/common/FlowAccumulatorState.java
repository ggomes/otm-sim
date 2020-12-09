package common;

import java.util.HashMap;
import java.util.Map;

public class FlowAccumulatorState {

    public Map<State,Double> count = new HashMap<>();     // key -> count

    public void reset(){
        for(State state : count.keySet())
            count.put(state,0d);
    }

    public void add_state(State state){
        if(!count.containsKey(state))
            count.put(state,0d);
    }

    public void increment(State state, Double x){
        if(!count.containsKey(state) || x.isNaN())
            return;
        count.put(state,x + count.get(state));
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
