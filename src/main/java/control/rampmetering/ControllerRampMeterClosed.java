package control.rampmetering;

import actuator.ActuatorMeter;
import common.Scenario;
import control.AbstractController;
import control.command.CommandNumber;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Controller;

public class ControllerRampMeterClosed extends AbstractControllerRampMetering {

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ControllerRampMeterClosed(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);
        this.has_queue_control = false;
    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
        update_command(scenario.dispatcher);
    }

    @Override
    protected float compute_nooverride_rate_vps(ActuatorMeter act,float timestamp) {
        return 0f;
    }
}
