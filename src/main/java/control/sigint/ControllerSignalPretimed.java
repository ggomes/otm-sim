package control.sigint;

import actuator.sigint.ActuatorSignal;
import actuator.sigint.BulbColor;
import control.AbstractController;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Controller;
import runner.Scenario;
import utils.CircularList;
import utils.OTMUtils;

import java.util.HashMap;
import java.util.Map;

public class ControllerSignalPretimed extends AbstractController {

    public float cycle = Float.NaN;
    public float offset = Float.NaN;
    public float start_time = Float.NaN;

    public int curr_stage_index;
    public CircularList<Stage> stages = new CircularList<>();

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
        int index = 0;
        if(jaxb_controller.getStages()!=null)
            for (jaxb.Stage stage : jaxb_controller.getStages().getStage())
                stages.add(new Stage(index++,stage));

        // set start_time
        float relstarttime = 0f;
        for(Stage stage : stages.queue){
            stage.cycle_starttime = relstarttime%cycle;
            relstarttime += stage.duration;
        }

    }

    ///////////////////////////////////////////////////
    // initialize
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

        if(stages.queue.isEmpty()){
            errorLog.addError("stages.queue.isEmpty()");
        } else {

            // cycle = sum durations
            double total_duration = stages.queue.stream().mapToDouble(x->x.duration).sum();
            if(!OTMUtils.approximately_equals(cycle,total_duration))
                errorLog.addError("cycle does not equal total stage durations: cycle=" + cycle + " , total_duration=" + total_duration);

            for (Stage stage : stages.queue)
                stage.validate(errorLog);
        }
    }

    @Override
    public void initialize(Scenario scenario, float now) throws OTMException {

    }

    ///////////////////////////////////////////////////
    // update
    ///////////////////////////////////////////////////

    @Override
    public void update_controller(Dispatcher dispatcher, float timestamp) throws OTMException {
        
        StageindexReltime x = get_stage_for_time(timestamp);

        ActuatorSignal signal = (ActuatorSignal) actuators.values().iterator().next();

        Map<Long,BulbColor> c = get_command_for_stage_index(x.index);
        command.put(signal.id , c);

        // this actuator is not on a dt, need to manually process
        signal.process_controller_command(c,dispatcher,timestamp);

        // register next poke
        float next_stage_start = timestamp - x.reltime + stages.peek().duration;
        dispatcher.register_event(new EventPoke(dispatcher,2,next_stage_start,this));

    }

    ///////////////////////////////////////////////////
    // getter
    ///////////////////////////////////////////////////

    public ActuatorSignal get_signal(){
        return (ActuatorSignal) actuators.values().iterator().next();
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

    public void set_stage_index(float timestamp,int index, Dispatcher dispatcher) throws OTMException {

        curr_stage_index = index;
        stages.point_to_index(index);

        // build the command
        Map<Long, BulbColor> command = new HashMap<>();
        Stage stage = stages.peek();
        for(Long phase_id : stage.phase_ids)
            command.put(phase_id,BulbColor.GREEN);

        // send command to actuator
        get_signal().process_controller_command(command,dispatcher,timestamp);

        //  inform output writer
//        if(event_listener !=null)
//            event_listener.write(timestamp, new EventControllerXXX(timestamp,id,current_schedule_item_index));
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
        for(int e=0;e<stages.queue.size();e++){
            end_time = start_time + stages.queue.get(e).duration;
            if(end_time>reltime)
                return new StageindexReltime(e,reltime-start_time);
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
