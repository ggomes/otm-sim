/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package api.info;

import control.sigint.Stage;

public class StageInfo {

    public int order;
    public float duration;          // duration in seconds of the stage, including
    public float cycle_starttime;   // start time of this stage relative to

    public StageInfo(Stage x){
        this.order = x.order;
        this.duration = x.duration;
        this.cycle_starttime = x.cycle_starttime;
    }

    @Override
    public String toString() {
        return "StageInfo{" +
                "order=" + order +
                ", duration=" + duration +
                ", cycle_starttime=" + cycle_starttime +
                '}';
    }

}
