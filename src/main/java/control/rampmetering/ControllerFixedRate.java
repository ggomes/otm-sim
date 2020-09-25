package control.rampmetering;

import actuator.AbstractActuator;
import actuator.AbstractActuatorLanegroupCapacity;
import actuator.ActuatorMeter;
import common.LaneGroupSet;
import common.Link;
import control.command.CommandNumber;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Controller;
import common.Scenario;

public class ControllerFixedRate extends AbstractControllerRampMetering {

    private float rate_vps;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ControllerFixedRate(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);

        assert(this.actuators.size()==1);

        AbstractActuatorLanegroupCapacity act = (AbstractActuatorLanegroupCapacity) actuators.values().iterator().next();

        if(jaxb_controller.getParameters()!=null){
            for(jaxb.Parameter p : jaxb_controller.getParameters().getParameter()){
                if(p.getName().compareTo("rate_vphpl")==0)
                    rate_vps = act.total_lanes * Float.parseFloat(p.getValue())/3600f;
            }
        }
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
        update_command(scenario.dispatcher);
    }

    @Override
    protected float compute_nooverride_rate_vps(ActuatorMeter act,float timestamp) {
        return rate_vps;
    }

}
