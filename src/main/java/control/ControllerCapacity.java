package control;

import actuator.AbstractActuator;
import actuator.ActuatorCapacity;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Controller;
import runner.Scenario;

import java.util.HashMap;
import java.util.Map;

public class ControllerCapacity extends AbstractController  {

    public Map<Long,Float> actuator_rate_vph;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ControllerCapacity(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);
        actuator_rate_vph = new HashMap<>();
        for(AbstractActuator actuator : actuators.values()){
            actuator_rate_vph.put(actuator.getId(),0f);
        }
    }

    @Override
    public void initialize(Scenario scenario, float now) throws OTMException {

    }

    @Override
    public void update_controller(Dispatcher dispatcher, float timestamp) throws OTMException {
        for(Map.Entry<Long,Float> e : actuator_rate_vph.entrySet()){
            Long act_id = e.getKey();
            Float rate_vph = e.getValue();
            ActuatorCapacity actuator = (ActuatorCapacity) actuators.get(act_id);
            command.put(act_id, rate_vph / 3600f);
        }
    }

    public void set_rate_vph_for_actuator(Long id,Float rate_vph){
        actuator_rate_vph.put(id,rate_vph);
    }

}
