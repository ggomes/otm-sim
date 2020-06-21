package control.rampmetering;

import control.AbstractController;
import control.command.CommandNumber;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Controller;
import common.Scenario;

public class ControllerFixedRate extends AbstractController {

    private float rate_vphpl;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ControllerFixedRate(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);

        // TODO READ rate_vphpl
        rate_vphpl = 100f;
    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {
        System.out.println(String.format("%.2f\tupdate_command\t%s",scenario.dispatcher.current_time,this.getClass().getName()));
        this.command.put(actuators.keySet().iterator().next(),new CommandNumber(rate_vphpl));
    }

    public void set_rate_vphpl(float x){
        rate_vphpl = x;
    }

}
