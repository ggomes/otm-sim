package control.rampmetering;

import actuator.ActuatorLaneGroupCapacity;
import core.Scenario;
import error.OTMException;
import jaxb.Controller;

public class ControllerRampMeterOpen extends AbstractControllerRampMetering {

    public ControllerRampMeterOpen(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);
        this.has_queue_control = false;
    }

    @Override
    protected float compute_nooverride_rate_vps(ActuatorLaneGroupCapacity act, float timestamp) {
        return Float.POSITIVE_INFINITY;
    }

}
