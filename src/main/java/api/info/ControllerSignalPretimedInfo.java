package api.info;

import control.sigint.ControllerSignalPretimed;
import control.sigint.Stage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ControllerSignalPretimedInfo extends ControllerInfo {

    public float start_time;
    public float cycle;
    public float offset;
    public List<StageInfo> stages = new ArrayList<>();

    public ControllerSignalPretimedInfo(ControllerSignalPretimed x){
        super(x);
        this.start_time = x.start_time;
        this.cycle = x.cycle;
        this.offset = x.offset;
        for(Stage xstage : x.stages){
            StageInfo stage = new StageInfo();
            stage.duration = xstage.duration;
            stage.phases.addAll(xstage.phase_ids);
            this.stages.add(stage);
        }
    }

    public float getStart_time() {
        return start_time;
    }

    public float getCycle() {
        return cycle;
    }

    public float getOffset() {
        return offset;
    }

    public List<StageInfo> getStages() {
        return stages;
    }

    public class StageInfo {
          public Set<Long> phases = new HashSet<>();
          public float duration;
          public float getDuration(){
              return duration;
          }
          public Set<Long> getPhases(){
              return phases;
          }
    }

}
