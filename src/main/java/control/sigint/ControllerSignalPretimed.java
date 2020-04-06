package control.sigint;

import actuator.sigint.ActuatorSignal;
import actuator.sigint.BulbColor;
import control.AbstractController;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Controller;
import common.Scenario;
import utils.OTMUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControllerSignalPretimed extends AbstractController {

    public float cycle = Float.NaN;
    public float offset = Float.NaN;
    public float start_time = Float.NaN;
    public List<Stage> stages = new ArrayList<>();
    public int curr_stage_index;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ControllerSignalPretimed(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);

        // parameters
        if(jaxb_controller.getParameters()!=null){
            for(jaxb.Parameter p : jaxb_controller.getParameters().getParameter()){
                switch(p.getName().toLowerCase()){
                    case "start_time":
                        this.start_time = Float.parseFloat(p.getValue());
                        break;
                    case "cycle":
                        this.cycle = Float.parseFloat(p.getValue());
                        break;
                    case "offset":
                        this.offset = Float.parseFloat(p.getValue());
                        break;
                }
            }
        }

        // stages
        if(jaxb_controller.getStages()!=null)
            for (jaxb.Stage stage : jaxb_controller.getStages().getStage())
                stages.add(new Stage(stage));

        // set start_time
        float relstarttime = 0f;
        for(Stage stage : stages){
            stage.cycle_starttime = relstarttime%cycle;
            relstarttime += stage.duration;
        }

    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);

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

        if(stages.isEmpty()){
            errorLog.addError("stages.queue.isEmpty()");
        } else {

            // cycle = sum durations
            double total_duration = stages.stream().mapToDouble(x->x.duration).sum();
            if(!OTMUtils.approximately_equals(cycle,total_duration))
                errorLog.addError("cycle does not equal total stage durations: cycle=" + cycle + " , total_duration=" + total_duration);

            for (Stage stage : stages)
                stage.validate(errorLog);
        }
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {

    }

    ///////////////////////////////////////////////////
    // update
    ///////////////////////////////////////////////////

    @Override
    public void update_controller(Dispatcher dispatcher, float timestamp) throws OTMException {

        StageindexReltime x = get_stage_for_time(timestamp);

        set_stage_index(x.index,timestamp);

        // register next poke
        float next_stage_start = timestamp - x.reltime + stages.get(x.index).duration;
        dispatcher.register_event(new EventPoke(dispatcher,2,next_stage_start,this));

    }

    ///////////////////////////////////////////////////
    // getter
    ///////////////////////////////////////////////////

    public ActuatorSignal get_signal(){
        return (ActuatorSignal) actuators.values().iterator().next();
    }

    public Integer get_stage_index(){
        return curr_stage_index;
    }

    public float get_cycle_time(float time){
        return (time-offset)%cycle;
    }

    ///////////////////////////////////////////////////
    // setter
    ///////////////////////////////////////////////////

    public Map<Long,BulbColor> get_command_for_stage_index(int index) {
        Map<Long, BulbColor> command = new HashMap<>();
        for(Long phase_id : stages.get(index).phase_ids)
            command.put(phase_id,BulbColor.GREEN);
        return command;
    }

    public void set_stage_index(int index, float timestamp) throws OTMException {

        curr_stage_index = index;

        ActuatorSignal signal = get_signal();
        Map<Long,BulbColor> c = get_command_for_stage_index(index);
        command.put(signal.id , c);

        // send command to actuator
        get_signal().process_controller_command(c,timestamp);

    }

    /** TEMPORARY **/
    public void set_stage_index(int index) throws OTMException {
        set_stage_index(index,this.scenario.get_current_time());
    }

    ///////////////////////////////////////////////////
    // private
    ///////////////////////////////////////////////////

    // for an absolute time value, returns the stage index and time
    // relative to the beginning of the cycle (offset time).
    // Assumes periodic extension in both directions.
    private StageindexReltime get_stage_for_time(float time){

        if(time<this.start_time || time>this.end_time)
            return null;

        float reltime = (time-offset)%cycle;
        float start_time = 0f;
        float end_time;

        for(int index=0;index<stages.size();index++){
            Stage stage = stages.get(index);
            end_time = start_time + stage.duration;
            if(end_time>reltime)
                return new StageindexReltime(index,reltime-start_time);
            start_time = end_time;
        }

        return new StageindexReltime(0,0);
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
