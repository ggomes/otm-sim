package control.commodity;

import common.Scenario;
import control.AbstractController;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Controller;

public class ControllerLaneChange extends AbstractController {

    public ControllerLaneChange(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);
    }

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {

    }
}
