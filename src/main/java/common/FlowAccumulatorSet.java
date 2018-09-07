/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
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
