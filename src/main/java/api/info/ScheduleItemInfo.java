/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package api.info;

import control.sigint.ScheduleItem;

import java.util.ArrayList;
import java.util.List;

public class ScheduleItemInfo {

    public float cycle;
    public float offset;
    public float start_time;
    public List<StageInfo> stages;

    public ScheduleItemInfo(ScheduleItem x){
        this.cycle = x.cycle;
        this.offset = x.offset;
        this.start_time = x.start_time;
        this.stages = new ArrayList<>();
//        for(Stage stage : x.stages)
//            this.stages.add(new StageInfo(stage));
    }

    public float getCycle() {
        return cycle;
    }

    public float getOffset() {
        return offset;
    }

    public float getStart_time() {
        return start_time;
    }

    public List<StageInfo> getStages() {
        return stages;
    }
}
