package control.sigint;

import error.OTMErrorLog;
import utils.OTMUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Stage {

    public float duration;          // duration in seconds of the stage, including
    public Set<Long> phase_ids;
    public float cycle_starttime;   // start time of this stage relative to

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public Stage(jaxb.Stage jaxb_stage){
        this.duration = jaxb_stage.getDuration();
        this.phase_ids = new HashSet<>();
        this.phase_ids.addAll(OTMUtils.csv2longlist(jaxb_stage.getPhases()));
    }

    public Stage(float duration, Long[] phase_ids){
        this.duration = duration;
        List<Long> phase_array = Arrays.asList(phase_ids);
        this.phase_ids = new HashSet<>(phase_array);
    }

    public void validate(OTMErrorLog errorLog){

        if(cycle_starttime<0)
            errorLog.addError("cycle_starttime<0");

    }

}
