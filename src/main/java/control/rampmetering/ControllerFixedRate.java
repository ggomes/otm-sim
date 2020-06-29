package control.rampmetering;

import actuator.AbstractActuator;
import actuator.AbstractActuatorLanegroupCapacity;
import common.AbstractLaneGroup;
import common.Link;
import control.AbstractController;
import control.command.CommandNumber;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Controller;
import common.Scenario;

import java.util.HashMap;
import java.util.Map;

public class ControllerFixedRate extends AbstractController {

    private float rate_vpspl;

    // queue control
    private boolean has_queue_control;
    private Map<Long,Float> queue_threshold;
    private float max_rate_vpspl;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ControllerFixedRate(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);

        has_queue_control = false;
        max_rate_vpspl = Float.POSITIVE_INFINITY;
        if(jaxb_controller.getParameters()!=null){
            for(jaxb.Parameter p : jaxb_controller.getParameters().getParameter()){
                if(p.getName().compareTo("rate_vphpl")==0)
                    rate_vpspl = Float.parseFloat(p.getValue())/3600f;
                if(p.getName().compareTo("max_rate_vphpl")==0)
                    max_rate_vpspl = Float.parseFloat(p.getValue())/3600f;
                if(p.getName().compareTo("queue_control")==0)
                    has_queue_control = Boolean.parseBoolean(p.getValue());
            }
        }

    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);

        queue_threshold = new HashMap<>();
        for(AbstractActuator act : actuators.values()) {
            Link orlink = ((AbstractLaneGroup) act.target).link;
            this.queue_threshold.put(act.id,orlink.road_param.getJamDensity() * orlink.full_lanes * orlink.length / 1000);
        }

        update_command(scenario.dispatcher);

        for(AbstractActuator act : actuators.values())
            this.command.put(act.id, new CommandNumber(((AbstractActuatorLanegroupCapacity) act).total_lanes * rate_vpspl));

    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {

        for(AbstractActuator act : actuators.values()) {
            double queue = has_queue_control ? ((AbstractLaneGroup) act.target).link.get_veh() : 0f;
            float thresh = queue_threshold.get(act.id);
            float qrate_vpspl = queue<thresh ? rate_vpspl : max_rate_vpspl ;
            this.command.put(act.id, new CommandNumber(((AbstractActuatorLanegroupCapacity) act).total_lanes * qrate_vpspl));
        }

    }

}
