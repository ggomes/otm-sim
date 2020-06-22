package control.rampmetering;

import actuator.AbstractActuator;
import actuator.AbstractActuatorLanegroupCapacity;
import control.AbstractController;
import control.command.CommandNumber;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Controller;
import common.Scenario;

public class ControllerFixedRate extends AbstractController {

    private float rate_vphpl = Float.POSITIVE_INFINITY;
    private boolean in_queue_override;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ControllerFixedRate(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);

        if(jaxb_controller.getParameters()!=null){
            for(jaxb.Parameter p : jaxb_controller.getParameters().getParameter()){
                if(p.getName().compareTo("rate_vphpl")==0){
                    rate_vphpl = Float.parseFloat(p.getValue());
                }
            }
        }
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);

        in_queue_override = true;
        update_command(scenario.dispatcher);
    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {

        if(in_queue_override){



            // check queue
            float queue = 0f;

            // if queue > threshold keep max rate
            float threshold = Float.POSITIVE_INFINITY;
            float rate_vpspl;
            if(queue>threshold){
                rate_vpspl = Float.POSITIVE_INFINITY;
            } else {
                rate_vpspl = rate_vphpl / 3600f;
                in_queue_override = false;
            }

            for(AbstractActuator act : actuators.values()) {

                System.out.println(String.format("%.2f\t Fixed rate: %f",scenario.dispatcher.current_time,rate_vpspl));


                this.command.put(act.id, new CommandNumber(((AbstractActuatorLanegroupCapacity) act).total_lanes * rate_vpspl));
            }

        }

    }

    public void set_rate_vphpl(float x){
        rate_vphpl = x;
    }

}
