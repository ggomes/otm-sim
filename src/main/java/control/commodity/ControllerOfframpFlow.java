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
import sensor.FixedSensor;
import utils.OTMUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ControllerOfframpFlow extends AbstractController {

    private FixedSensor sensor;
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

        // sensor
        ActuatorSplit act = (ActuatorSplit) actuators.values().iterator().next();
        Set<Long> commids = new HashSet<>();
        commids.add(act.comm.getId());
        this.sensor = new FixedSensor(dt, act.linkin,1,act.linkin.full_lanes,act.linkin.length,commids);
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);

        if(actuators.size()!=1)
            errorLog.addError("Offramp flow controller must have exactly one actuator.");
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
        sensor.initialize(scenario);
    }

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {

        ActuatorSplit act = (ActuatorSplit) actuators.values().iterator().next();

        // get the flow that is currently entering linkin for this commodity
        double flow_in_vph = sensor.get_flow_vph();

        // get the desired exit flow
        double des_flow_out_vph = ref.get_value_for_time(dispatcher.current_time);

        // compute the split ratio
        float split = flow_in_vph==0f ? 1f : (float) ( des_flow_out_vph / flow_in_vph );
        if(split<0f)
            split = 0f;
        if(split>1f)
            split = 1f;

        if(split>1)
            split=1f;

        // update the command
        command.put(act.id, new CommandNumber(split));
    }
}
