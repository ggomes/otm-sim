package api.info;

import control.sigint.Stage;

import java.util.HashSet;
import java.util.Set;

public class StageInfo implements Comparable<StageInfo> {

    public int index;
    public float duration;          // duration in seconds of the stage, including
    public Set<Long> phases;

    public StageInfo(Stage x){
        this.index = x.index;
        this.duration = x.duration;
        this.phases = new HashSet<>();
        this.phases.addAll(x.phase_ids);
    }

    public int getIndex() {
        return index;
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
                "index=" + index +
                ", duration=" + duration +
                '}';
    }

    @Override
    public int compareTo(StageInfo o) {
        return Integer.compare(index,o.index);
    }
}
