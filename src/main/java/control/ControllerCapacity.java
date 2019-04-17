package control;

import actuator.ActuatorCapacity;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Controller;
import runner.Scenario;

import java.util.HashMap;
import java.util.Map;

public class ControllerCapacity extends AbstractController  {

    public Map<String,Float> actuator_rate_vph;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ControllerCapacity(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);
        actuator_rate_vph = new HashMap<>();
    }

    @Override
    public void initialize(Scenario scenario, float now) throws OTMException {
        for(String actuator_name : actuator_by_usage.keySet())
            actuator_rate_vph.put(actuator_name,0f);
    }

    @Override
    public void update_controller(Dispatcher dispatcher, float timestamp) throws OTMException {
        for(Map.Entry<String,Float> e : actuator_rate_vph.entrySet())
            ((ActuatorCapacity) actuator_by_usage.get(e.getKey())).rate_vps = e.getValue() / 3600f;
    }
}
