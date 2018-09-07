/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package common;

public class FlowAccumulator {

    public final AbstractLaneGroup lanegroup;
    public final long commodity_id;
    public final boolean is_global;
    public float vehicle_count;

    public FlowAccumulator(AbstractLaneGroup lanegroup, long commodity_id) {
        this.lanegroup = lanegroup;
        this.commodity_id = commodity_id;
        this.is_global = false;
    }

    public FlowAccumulator(AbstractLaneGroup lanegroup) {
        this.lanegroup = lanegroup;
        this.commodity_id = -1;
        this.is_global = true;
    }

    public void increment(double x){
        this.vehicle_count += x;
    }

    public void reset(){
        vehicle_count = 0f;
    }

}
