package control.sigint;

import actuator.ActuatorSignal;
import actuator.SignalPhase;
import control.AbstractController;
import control.command.CommandSignal;
import core.Scenario;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Controller;
import output.events.EventWrapperController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ControllerSignalFollower extends AbstractController {

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ControllerSignalFollower(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);
    }

    @Override
    protected void configure() throws OTMException {
    }

    @Override
    protected void update_command(Dispatcher dispatcher) throws OTMException {
    }

    @Override
    public Class get_actuator_class() {
        return ActuatorSignal.class;
    }

    public void set_active_phases(ArrayList<Integer> green_phases) throws OTMException {

        if(!is_on)
            return;

        float now = this.scenario.dispatcher.current_time;

        // cast as CommandSignal
        ActuatorSignal signal = (ActuatorSignal) this.actuators.values().iterator().next();
        Map<Long, SignalPhase.BulbColor> value = new HashMap<>();
        for( long phase : green_phases )
            value.put(phase, SignalPhase.BulbColor.GREEN);
        CommandSignal sig_command = new CommandSignal(value);

        // save in controller
        command.put(signal.id, sig_command);

        // send to actuator
        signal.process_command(sig_command,now);

        // write to output
        if(event_output!=null)
            event_output.write(new EventWrapperController(now,command));
    }

}
