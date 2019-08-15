package actuator.sigint;

import actuator.AbstractActuator;
import control.sigint.ScheduleItem;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import runner.Scenario;

import java.util.HashMap;
import java.util.Map;

public class ActuatorSignalSimple extends AbstractActuator {

    public Map<Long, SignalPhaseSimple> signal_phases;
    public ScheduleItem current_schedule_item;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ActuatorSignalSimple(Scenario scenario, jaxb.Actuator jaxb_actuator) throws OTMException {
        super(scenario,jaxb_actuator);

        // must be on a node
        if(target==null || !(target instanceof common.Node))
            return;

        if(jaxb_actuator.getSignal()==null)
            return;

        signal_phases = new HashMap<>();
        for(jaxb.Phase jaxb_phase : jaxb_actuator.getSignal().getPhase())
            signal_phases.put(jaxb_phase.getId(), new SignalPhaseSimple(scenario, this, jaxb_phase));

    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);

        if(signal_phases==null)
            errorLog.addError("ActuatorSignalSimple ID=" + id + " contains no valid phases.");
        else
            for(SignalPhaseSimple p : signal_phases.values())
                p.validate(errorLog);
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        float now = scenario.get_current_time();
        // set all bulb colors to dark
        for(SignalPhaseSimple p : signal_phases.values() )
            p.initialize(now);
    }

    @Override
    public void poke(Dispatcher dispatcher, float timestamp) throws OTMException {
        super.poke(dispatcher, timestamp);
    }

    public void turn_off(float now) throws OTMException {
        for(SignalPhaseSimple p : signal_phases.values() )
            p.turn_off(now);
    }

    public SignalPhaseSimple get_phase(long phase_id){
        return signal_phases.get(phase_id);
    }

    public void enable_phase(long phase_id, float time) throws OTMException {
        if (!signal_phases.keySet().contains(phase_id)){
            throw new OTMException(
                String.format("phase ID %s is not in actuator %s", phase_id, id));
        } else {
            signal_phases.values().forEach(p -> p.disable(time));
            signal_phases.values().stream().filter(p -> p.id == phase_id).forEach(p -> p.enable(time));
        }        
    }

    ///////////////////////////////////////////////////
    // control
    ///////////////////////////////////////////////////

    @Override
    public void process_controller_command(Object command, Dispatcher dispatcher, float timestamp) throws OTMException {

        if (command != null){
            enable_phase((long) command, timestamp);
        } else {
            // null command triggers turning off the signal,
            // i.e. allow traffic to pass for all phases.
            turn_off(timestamp);
        }

     }

}