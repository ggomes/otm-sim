package control.rampmetering;

import actuator.AbstractActuator;
import actuator.ActuatorMeter;
import common.LaneGroupSet;
import common.Link;
import common.Scenario;
import control.AbstractController;
import control.command.CommandNumber;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractControllerRampMetering extends AbstractController {

    protected float max_rate_vpspl;
    protected float min_rate_vpspl;

    // queue control
    protected boolean has_queue_control;
    protected float override_threshold;
    protected Map<Long,Float> queue_threshold;

    protected abstract float compute_nooverride_rate_vps(ActuatorMeter act,float timestamp);

    public AbstractControllerRampMetering(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);

        has_queue_control = false;
        min_rate_vpspl = Float.NEGATIVE_INFINITY;
        max_rate_vpspl = Float.POSITIVE_INFINITY;
        if(jaxb_controller.getParameters()!=null){
            for(jaxb.Parameter p : jaxb_controller.getParameters().getParameter()){
                if(p.getName().compareTo("max_rate_vphpl")==0)
                    max_rate_vpspl = Float.parseFloat(p.getValue())/3600f;
                if(p.getName().compareTo("min_rate_vphpl")==0)
                    min_rate_vpspl = Float.parseFloat(p.getValue())/3600f;
                if(p.getName().compareTo("queue_control")==0)
                    has_queue_control = Boolean.parseBoolean(p.getValue());
                if(p.getName().compareTo("override_threshold")==0)
                    override_threshold = Float.parseFloat(p.getValue());
            }
        }

    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);

        queue_threshold = new HashMap<>();
        for(AbstractActuator act : actuators.values()) {
            // all lanegroups in the actuator must be in the same link
            LaneGroupSet lgs = (LaneGroupSet)act.target;
            Set<Link> ors = lgs.lgs.stream().map(lg->lg.link).collect(Collectors.toSet());
            if(ors.size()!=1)
                throw new OTMException("All lanegroups in any single actuator used by a Fixed rate controller must belong to the same link.");
            Link orlink = ors.iterator().next();
            this.queue_threshold.put(act.id,override_threshold * orlink.road_param.getJamDensity() * orlink.full_lanes * orlink.length / 1000);
        }

    }

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {
        for(AbstractActuator abs_act : actuators.values()) {
            float rate_vps = compute_nooverride_rate_vps((ActuatorMeter) abs_act,dispatcher.current_time);
            if(has_queue_control){
                Link or = ((LaneGroupSet) abs_act.target).lgs.iterator().next().link;
                if(or.get_veh() >= queue_threshold.get(abs_act.id))
                    rate_vps = max_rate_vpspl;
            }
            this.command.put(abs_act.id, new CommandNumber(rate_vps));
        }
    }
}
