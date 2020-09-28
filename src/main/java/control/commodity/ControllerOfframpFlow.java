package control.commodity;

import actuator.ActuatorSplit;
import common.Scenario;
import control.AbstractController;
import control.command.CommandNumber;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Controller;
import jaxb.Parameter;
import profiles.Profile1D;
import utils.OTMUtils;

import java.util.ArrayList;
import java.util.List;

public class ControllerOfframpFlow extends AbstractController {

    private Profile1D ref;

    public ControllerOfframpFlow(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);

        Float refdt = Float.NaN;
        List<Double> values = new ArrayList<>();
        if(jaxb_controller.getParameters()!=null){
            for(Parameter p : jaxb_controller.getParameters().getParameter()){
                switch(p.getName()){
                    case "dt":
                        refdt = Float.parseFloat(p.getValue());
                        break;
                    case "flowvph":
                        values = OTMUtils.csv2list(p.getValue());
                        break;
                }
            }
        }
        this.ref = new Profile1D(start_time,refdt,values);
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);

        if(actuators.size()!=1)
            errorLog.addError("Offramp flow controller must have exactly one actuator.");
    }

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {
        System.out.println(String.format("%.1f\tControllerOfframpFlow\tupdate_command",scenario.dispatcher.current_time));


        ActuatorSplit act = (ActuatorSplit) actuators.values().iterator().next();

        // get the flow that is currently entering linkin for this commodity
        double flow_in_vps = 1000f;

        // get the desired exit flow
        double des_flow_out_vps = ref.get_value_for_time(dispatcher.current_time)/3600f;

        // compute the split ratio
        float split = flow_in_vps==0f ? 1f : (float) ( des_flow_out_vps / flow_in_vps );
        if(split<0f)
            split = 0f;
        if(split>1f)
            split = 1f;

        // update the command
        command.put(act.id, new CommandNumber(split));
    }
}
