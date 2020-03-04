package api.info;

import control.sigint.Stage;

import java.util.HashSet;
import java.util.Set;

public class StageInfo implements Comparable<StageInfo> {

    public int order;
    public float duration;          // duration in seconds of the stage, including
    public Set<Long> phases;

    public StageInfo(Stage x){
        this.order = x.order;
        this.duration = x.duration;
        this.phases = new HashSet<>();
        this.phases.addAll(x.phase_ids);
    }

    public int getOrder() {
        return order;
    }

    public float getDuration() {
        return duration;
    }

    public Set<Long> getPhases() {
        return phases;
    }

    @Override
    public String toString() {
        return "StageInfo{" +
                "order=" + order +
                ", duration=" + duration +
                '}';
    }

    @Override
    public int compareTo(StageInfo o) {
        return Integer.compare(order,o.order);
    }
}
