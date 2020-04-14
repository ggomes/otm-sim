package actuator.sigint;

import actuator.AbstractActuatorLanegroupCapacity;
import error.OTMErrorLog;
import error.OTMException;
import common.Scenario;

import java.util.HashMap;
import java.util.Map;

public class ActuatorSignal extends AbstractActuatorLanegroupCapacity {

    public Map<Long, SignalPhase> signal_phases;

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

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

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

    ///////////////////////////////////////////////////
    // AbstractActuator
    ///////////////////////////////////////////////////

    @Override
    public void process_controller_command(Object obj, float timestamp) throws OTMException {

        // The command is a map from signal phase to color.
        // anything not in the map should be set to red
        Map<Long,BulbColor> command = (Map<Long,BulbColor>) obj;

        for( Map.Entry<Long, SignalPhase> e : signal_phases.entrySet()){
            long phase_id = e.getKey();
            SignalPhase phase = e.getValue();
            BulbColor bulbcolor = command.containsKey(phase_id) ? command.get(phase_id) : BulbColor.RED;
            phase.set_bulb_color(timestamp, bulbcolor);
        }

    }

    ///////////////////////////////////////////////////
    // get
    ///////////////////////////////////////////////////

    public SignalPhase get_phase(long phase_id){
        return signal_phases.get(phase_id);
    }

}
