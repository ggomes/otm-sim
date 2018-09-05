package common;

import java.util.HashSet;
import java.util.Set;

public class FlowAccumulatorSet {
    public Set<FlowAccumulator> flow_accumulators;

    public FlowAccumulatorSet(){
        flow_accumulators = new HashSet<>();
    }

    public void add_flow_accumulator(FlowAccumulator fa){
        this.flow_accumulators.add(fa);
    }

    public float get_vehicle_count(){
        float r = 0f;
        for(FlowAccumulator f: flow_accumulators)
            r += f.vehicle_count;
        return r;
    }

    public void reset(){
        flow_accumulators.forEach(x->x.reset());
    }

}
