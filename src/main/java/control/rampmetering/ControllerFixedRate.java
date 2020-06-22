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

public class ControllerFixedRate extends AbstractController {

    private float rate_vpspl;

    // queue control
    private boolean has_queue_control;
    private float queue_threshold;
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

        queue_threshold = Float.POSITIVE_INFINITY;

    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);

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
            float qrate_vpspl = queue>queue_threshold ? max_rate_vpspl : rate_vpspl;
            this.command.put(act.id, new CommandNumber(((AbstractActuatorLanegroupCapacity) act).total_lanes * qrate_vpspl));
        }

    }

}
