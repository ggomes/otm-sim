package control.rampmetering;

import common.Scenario;
import control.AbstractController;
import control.command.CommandNumber;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Controller;

public class ControllerRampMeterOpen extends AbstractController {

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ControllerRampMeterOpen(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);
    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {
        this.command.put(actuators.keySet().iterator().next(),new CommandNumber(Float.POSITIVE_INFINITY));
    }

}
