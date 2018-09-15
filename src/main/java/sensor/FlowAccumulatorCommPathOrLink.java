package sensor;

import keys.KeyCommPathOrLink;

import java.util.HashMap;
import java.util.Map;

public class FlowAccumulatorCommPathOrLink {

    public Map<KeyCommPathOrLink,Double> count = new HashMap<>();

    public void reset(){
        count = new HashMap<>();
    }

    public void add_key(KeyCommPathOrLink key){
        if(!count.containsKey(key))
            count.put(key,0d);
    }

    public void increment(KeyCommPathOrLink key,Double x){
        if(x.isNaN())
            return;
        count.put(key,x + (count.containsKey(key)?count.get(key):0d));
    }

}
