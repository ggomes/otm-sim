package control.rampmetering;

import actuator.AbstractActuator;
import actuator.ActuatorLaneGroupCapacity;
import error.OTMException;
import jaxb.Controller;
import core.Scenario;

import java.util.HashMap;
import java.util.Map;

public class ControllerFixedRate extends AbstractControllerRampMetering {

    private float rate_vpspl;
    private Map<Long,Float> rate_vps;

    public ControllerFixedRate(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);
        if(jaxb_controller.getParameters()!=null){
            for(jaxb.Parameter p : jaxb_controller.getParameters().getParameter()){
                if(p.getName().compareTo("rate_vphpl")==0)
                    rate_vpspl = Float.parseFloat(p.getValue())/3600f;
            }
        }
    }

    @Override
    public void configure() throws OTMException{
        super.configure();

        rate_vps = new HashMap<>();
        for(AbstractActuator abs_act : actuators.values()){
            ActuatorLaneGroupCapacity act = (ActuatorLaneGroupCapacity) abs_act;
            rate_vps.put(act.id, act.total_lanes * rate_vpspl);
        }
    }

    @Override
    protected float compute_nooverride_rate_vps(ActuatorLaneGroupCapacity act, float timestamp) {
        return rate_vps.get(act.id);
    }

}
