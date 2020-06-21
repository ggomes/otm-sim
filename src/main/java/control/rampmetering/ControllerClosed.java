package control.rampmetering;

import common.Scenario;
import control.AbstractController;
import control.command.CommandNumber;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Controller;

public class ControllerClosed extends AbstractController {

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ControllerClosed(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);
    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {
        System.out.println(String.format("%.2f\tupdate_command\t%s",scenario.dispatcher.current_time,this.getClass().getName()));
        this.command.put(actuators.keySet().iterator().next(),new CommandNumber(0f));
    }

}
