package control.sigint;

import control.AbstractController;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Controller;
import runner.Scenario;
import utils.CircularList;

public class ControllerSignalPretimed extends AbstractController {

    public float cycle;
    public float offset;
    public float start_time;
    public CircularList<Stage> stages;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ControllerSignalPretimed(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);
    }


    ///////////////////////////////////////////////////
    // initialize
    ///////////////////////////////////////////////////


    @Override
    public void initialize(Scenario scenario, float now) throws OTMException {

    }


    ///////////////////////////////////////////////////
    // update
    ///////////////////////////////////////////////////

    @Override
    public void update_controller(Dispatcher dispatcher, float timestamp) throws OTMException {

    }
}
