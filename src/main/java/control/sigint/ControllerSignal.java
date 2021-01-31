package control.sigint;

import actuator.ActuatorSignal;
import actuator.SignalPhase;
import control.AbstractController;
import control.command.CommandSignal;
import core.Scenario;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControllerSignal extends AbstractController  {

    public List<Stage> stages = new ArrayList<>();
    public int curr_stage_index;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ControllerSignal(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);
        if(jaxb_controller.getStages()!=null)
            for (jaxb.Stage stage : jaxb_controller.getStages().getStage())
                stages.add(new Stage(stage));
    }

    @Override
    public Class get_actuator_class() {
        return ActuatorSignal.class;
    }

    @Override
    protected void configure() throws OTMException {

    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void validate_pre_init(OTMErrorLog errorLog) {
        super.validate_pre_init(errorLog);

        if(stages.isEmpty()){
            errorLog.addError("stages.queue.isEmpty()");
            return;
        }

        for (Stage stage : stages)
            stage.validate(errorLog);
    }

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {

    }

    ///////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////

    public final ActuatorSignal get_signal(){
        return (ActuatorSignal) actuators.values().iterator().next();
    }

    public final Integer get_stage_index(){
        return curr_stage_index;
    }

    public final CommandSignal get_command_for_stage_index(int index) {
        Map<Long, SignalPhase.BulbColor> command = new HashMap<>();
        for(Long phase_id : stages.get(index).phase_ids)
            command.put(phase_id, SignalPhase.BulbColor.GREEN);
        return new CommandSignal(command);
    }

    public final void set_stage_index(int index) throws OTMException {

        curr_stage_index = index;

        ActuatorSignal signal = get_signal();
        CommandSignal c = get_command_for_stage_index(index);
        command.put(signal.id , c);

        // send command to actuator
        get_signal().process_command(c,scenario.get_current_time());

    }

}
