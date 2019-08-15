package actuator.sigint;

import actuator.AbstractActuator;
import control.sigint.ScheduleItem;
import control.sigint.Stage;
import dispatch.Dispatcher;
import dispatch.EventAdvanceSignalPhase;
import error.OTMErrorLog;
import error.OTMException;
import runner.Scenario;

import java.util.HashMap;
import java.util.Map;

public class ActuatorSignal extends AbstractActuator {

    public Map<Long, SignalPhase> signal_phases;
    public ScheduleItem current_scehdule_item;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ActuatorSignal(Scenario scenario, jaxb.Actuator jaxb_actuator) throws OTMException {
        super(scenario,jaxb_actuator);

        // must be on a node
        if(target==null || !(target instanceof common.Node))
            return;

        if(jaxb_actuator.getSignal()==null)
            return;

        signal_phases = new HashMap<>();
        for(jaxb.Phase jaxb_phase : jaxb_actuator.getSignal().getPhase())
            signal_phases.put(jaxb_phase.getId(), new SignalPhase(scenario, this, jaxb_phase));

    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);

        if(signal_phases==null)
            errorLog.addError("ActuatorSignal ID=" + id + " contains no valid phases.");
        else
            for(SignalPhase p : signal_phases.values())
                p.validate(errorLog);
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {

        float now = scenario.get_current_time();

        // set all bulb colors to dark
        for(SignalPhase p : signal_phases.values() )
            p.initialize(now);
    }

    public void turn_off(float now) throws OTMException {
        for(SignalPhase p : signal_phases.values() )
            p.turn_off(now);
    }

    ///////////////////////////////////////////////////
    // getters
    ///////////////////////////////////////////////////

    public SignalPhase get_phase(long phase_id){
        return signal_phases.get(phase_id);
    }

    public float cycle(){
        return current_scehdule_item==null ? Float.NaN : current_scehdule_item.cycle;
    }

    ///////////////////////////////////////////////////
    // control
    ///////////////////////////////////////////////////

    @Override
    public void process_controller_command(Object command, Dispatcher dispatcher, float timestamp) throws OTMException {

        if (command != null){
            current_scehdule_item = (ScheduleItem) command;
            float cycle_time = current_scehdule_item.get_cycle_time(timestamp);

            // remove future phase transition events from the dispatcher
            for(SignalPhase phase : signal_phases.values())
                dispatcher.remove_events_for_recipient(EventAdvanceSignalPhase.class,phase);

            // clear phase transitions
            signal_phases.values().forEach(x->x.transitions.clear());

            // generate new green/red transitions
            for(Stage stage:current_scehdule_item.stages.queue){
                float r2g = stage.cycle_starttime;
                float g2r = (r2g + stage.duration) % current_scehdule_item.cycle;
                for(long phase_id : stage.phase_ids){
                    SignalPhase phase = signal_phases.get(phase_id);
                    phase.transitions.add(new PhaseTransition(r2g,BulbColor.RED,BulbColor.GREEN));
                    phase.transitions.add(new PhaseTransition(g2r,BulbColor.GREEN,BulbColor.RED));
                }
            }

            for(SignalPhase phase : signal_phases.values()) {

                if(phase.transitions.queue.isEmpty())
                    continue;

                phase.cancel_redundate_transitions();
                phase.insert_yellow_time();

                // set state for current time ............
                PhaseTransition recent_transition = phase.get_transition_preceding(cycle_time);
                phase.transitions.point_to(recent_transition);
                phase.set_bulb_color(timestamp,recent_transition.to_color);

                // special case: next transition is now
                if(phase.transitions.peek_next().cycle_time==cycle_time)
                    phase.advance_transitions(timestamp);

                // execute next transition
                phase.register_next_transtions(dispatcher,timestamp,cycle_time);

            }
        } else {
            // null command triggers turning off the signal,
            // i.e. allow traffic to pass for all phases.
            turn_off(timestamp);
        }

     }

}
