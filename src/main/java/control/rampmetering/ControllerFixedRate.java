package control.rampmetering;

import actuator.AbstractActuator;
import actuator.AbstractActuatorLanegroupCapacity;
import actuator.ActuatorMeter;
import error.OTMException;
import jaxb.Controller;
import core.Scenario;

import java.util.HashMap;
import java.util.Map;

public class ControllerFixedRate extends AbstractControllerRampMetering {

    private float rate_vpspl;
    private Map<Long,Float> rate_vps;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

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
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
        rate_vps = new HashMap<>();
        for(AbstractActuator abs_act : actuators.values()){
            AbstractActuatorLanegroupCapacity act = (AbstractActuatorLanegroupCapacity) abs_act;
            rate_vps.put(act.id, act.total_lanes * rate_vpspl);
        }
    }

    @Override
    protected float compute_nooverride_rate_vps(ActuatorMeter act,float timestamp) {
        return rate_vps.get(act.id);
    }

}
