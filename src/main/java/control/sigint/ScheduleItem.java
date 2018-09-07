/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package control.sigint;

import error.OTMErrorLog;
import utils.OTMUtils;
import utils.CircularList;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ScheduleItem implements Comparable<ScheduleItem> {

    public float cycle;
    public float offset;
    public float start_time;
    public CircularList<Stage> stages;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ScheduleItem(jaxb.ScheduleItem jaxb_si){
        this.cycle = jaxb_si.getCycle();
        this.offset = jaxb_si.getOffset();
        this.start_time = jaxb_si.getStartTime();

        // create stages
        Set stage_collection = new HashSet<>();
        for(jaxb.Stage stage : jaxb_si.getStages().getStage() )
            stage_collection.add(new Stage(stage));
        stages = new CircularList<>(stage_collection);

        // set start_time
        float relstarttime = 0f;
        for(Stage stage : stages.queue){
            stage.cycle_starttime = relstarttime%cycle;
            relstarttime += stage.duration;
        }
    }

    public void validate(OTMErrorLog errorLog){

        // positivity
        if(cycle<=0)
            errorLog.addError("cycle<=0");

        if(offset<0)
            errorLog.addError("offset<0");

        if(start_time<0)
            errorLog.addError("start_time<0");

        // offset less than cycle
        if(offset>=cycle)
            errorLog.addError("offset>=cycle");

        if(stages.queue.isEmpty()){
            errorLog.addError("stages.queue.isEmpty()");
        } else {

            // cycle = sum durations
            double total_duration = stages.queue.stream().mapToDouble(x->x.duration).sum();
            if(!OTMUtils.approximately_equals(cycle,total_duration))
                errorLog.addError("cycle does not equal total stage durations: cycle=" + cycle + " , total_duration=" + total_duration);

            // orders are distinct
            Set<Integer> orders = stages.queue.stream().map(x->x.order).collect(Collectors.toSet());
            if(orders.size()!=stages.queue.size())
                errorLog.addError("stage orders are not distinct");

            for (Stage stage : stages.queue)
                stage.validate(errorLog);
        }

    }

    ///////////////////////////////////////////////////
    // getter
    ///////////////////////////////////////////////////

    public float get_cycle_time(float time){
        return (time-offset)%cycle;
    }

    // for an absolute time value, returns the stage index and time
    // relative to the beginning of the cycle (offset time).
    // Assumes periodic extension in both directions.
    private StageindexReltime get_stage_for_time(float time){
        float reltime = (time-offset)%cycle;
        float start_time = 0f;
        float end_time;
        for(int e=0;e<stages.queue.size();e++){
            end_time = start_time + stages.queue.get(e).duration;
            if(end_time>reltime)
                return new StageindexReltime(e,reltime-start_time);
            start_time = end_time;
        }
        return new StageindexReltime(0,0);
    }

    @Override
    public int compareTo(ScheduleItem other) {
        return Float.compare(this.start_time, other.start_time);
    }

    private class StageindexReltime{
        int index;
        float reltime;
        public StageindexReltime(int index,float reltime){
            this.index = index;
            this.reltime = reltime;
        }
    }

}
