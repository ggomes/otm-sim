/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package api.info;

import control.sigint.ControllerSignalPretimed;
import control.sigint.ScheduleItem;

import java.util.ArrayList;
import java.util.List;

public class ControllerSignalPretimedInfo {

    public List<ScheduleItemInfo> schedule;


    public ControllerSignalPretimedInfo(ControllerSignalPretimed x){
        this.schedule = new ArrayList<>();
        for(ScheduleItem item : x.schedule)
            this.schedule.add(new ScheduleItemInfo(item));
    }

    public List<ScheduleItemInfo> getSchedule() {
        return schedule;
    }

    @Override
    public String toString() {
        return "ControllerSignalPretimedInfo{" +
                "schedule=" + schedule +
                '}';
    }
}
