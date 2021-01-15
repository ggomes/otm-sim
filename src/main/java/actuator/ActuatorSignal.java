package actuator;

import control.command.CommandSignal;
import control.command.InterfaceCommand;
import core.ScenarioElementType;
import error.OTMErrorLog;
import error.OTMException;
import core.Scenario;

import java.util.HashMap;
import java.util.Map;

public class ActuatorSignal extends AbstractActuator {

    public Map<Long, SignalPhase> signal_phases;

    public ActuatorSignal(Scenario scenario, jaxb.Actuator jaxb_actuator) throws OTMException {
        super(scenario,jaxb_actuator);

        // must be on a node
        if(target==null)
            return;

        if(jaxb_actuator.getSignal()==null)
            return;

        signal_phases = new HashMap<>();
        for(jaxb.Phase jaxb_phase : jaxb_actuator.getSignal().getPhase())
            signal_phases.put(jaxb_phase.getId(), new SignalPhase(scenario, this, jaxb_phase));

    }

    @Override
    public Type getType() {
        return Type.signal;
    }

    @Override
    protected ScenarioElementType get_target_class() {
        return ScenarioElementType.node;
    }

    @Override
    public void validate_pre_init(OTMErrorLog errorLog) {
        super.validate_pre_init(errorLog);

        if(signal_phases==null)
            errorLog.addError("ActuatorSignal ID=" + id + " contains no valid phases.");
    }


    @Override
    public void validate_post_init(OTMErrorLog errorLog) {
        super.validate_post_init(errorLog);
        if(signal_phases!=null)
            for(SignalPhase p : signal_phases.values())
                p.validate_post_init(errorLog);
    }

    @Override
    public void initialize(Scenario scenario, float timestamp,boolean override_targets) throws OTMException {

        if(initialized)
            return;

        super.initialize(scenario,timestamp,override_targets);

        // set all bulb colors to dark
        for(SignalPhase p : signal_phases.values() )
            p.initialize(override_targets);

        // register the actuator
        target.register_actuator(commids,this,override_targets);
    }

    @Override
    public void process_command(InterfaceCommand command, float timestamp) throws OTMException {
        if(command==null)
            return;
        if(!(command instanceof CommandSignal))
            throw new OTMException("Bad command type.");

        // The command is a map from signal phase to color.
        // anything not in the map should be set to red
        Map<Long, SignalPhase.BulbColor> signalcommand = ((CommandSignal)command).value;
        for( Map.Entry<Long, SignalPhase> e : signal_phases.entrySet()){
            long phase_id = e.getKey();
            SignalPhase phase = e.getValue();
            SignalPhase.BulbColor bulbcolor = signalcommand.containsKey(phase_id) ? signalcommand.get(phase_id) : SignalPhase.BulbColor.RED;
            phase.set_bulb_color(bulbcolor);
        }
    }

    public SignalPhase get_phase(long phase_id){
        return signal_phases.get(phase_id);
    }

    @Override
    protected InterfaceCommand command_off() {
        return null;
    }


}
