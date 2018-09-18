package sensor;

import java.util.HashMap;
import java.util.Map;

public class AccumulatorCommodity {

    public Map<Long,Double> count = new HashMap<>();     // commodity -> count

    public void reset(){
        count = new HashMap<>();
    }

    public void add_commodity(Long commid){
        if(!count.containsKey(commid))
            count.put(commid,0d);
    }

    public void increment(Long commid, Double x){
        if(x.isNaN())
            return;
        count.put(commid,x + (count.containsKey(commid)?count.get(commid):0d));
    }

}
