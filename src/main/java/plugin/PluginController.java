package plugin;

import control.AbstractController;
import error.OTMException;
import jaxb.Controller;
import common.Scenario;

public abstract class PluginController extends AbstractController {

    public PluginController(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);
    }

}
