package plugin;

import actuator.AbstractActuator;
import control.AbstractController;
import error.OTMException;
import jaxb.Controller;
import core.Scenario;

public abstract class PluginController extends AbstractController {

    public PluginController(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);
    }


    @Override
    public Class get_actuator_class() {
        return AbstractActuator.class;
    }

}
