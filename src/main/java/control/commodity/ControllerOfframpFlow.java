package control.commodity;

import actuator.ActuatorSplit;
import common.Scenario;
import control.AbstractController;
import control.command.CommandDoubleMap;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Controller;
import profiles.Profile1D;
import sensor.FixedSensor;
import utils.OTMUtils;

import java.util.*;
import java.util.stream.Collectors;

public class ControllerOfframpFlow extends AbstractController {

    private FixedSensor ml_sensor;
    private Map<Long,Profile1D> fr_ref;
    private CommandDoubleMap splitmap;
    private long actid;

    public ControllerOfframpFlow(Scenario scenario, Controller jcon) throws OTMException {
        super(scenario, jcon);

        fr_ref = new HashMap<>();

        if(jcon.getProfiles()!=null){
            for(jaxb.Profile prof : jcon.getProfiles().getProfile()){
                float prof_start_time = prof.getStartTime();
                float prof_dt = prof.getDt();
                long prof_id = prof.getId();
                List<Double> values = OTMUtils.csv2list(prof.getContent());
                fr_ref.put(prof_id,new Profile1D(prof_start_time,prof_dt,values));
            }
        }

        // sensor
        ActuatorSplit act = (ActuatorSplit) actuators.values().iterator().next();
        this.actid = act.id;
        Set<Long> commids = new HashSet<>();
        commids.add(act.comm.getId());
        this.ml_sensor = new FixedSensor(dt, act.linkMLup,1,act.linkMLup.full_lanes,act.linkMLup.length,commids);
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);

        if(actuators.size()!=1)
            errorLog.addError("Offramp flow controller must have exactly one actuator.");

        this.fr_ref.values().forEach(p->p.validate(errorLog));

        ActuatorSplit act = (ActuatorSplit) actuators.values().iterator().next();
        Set<Long> act_linkids = act.linkFRs.stream().map(l->l.getId()).collect(Collectors.toSet());
        if(!act_linkids.equals(fr_ref.keySet()))
            errorLog.addError("Controller and actuator link ids dont match.");
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
        ml_sensor.initialize(scenario);

        Map<Long,Double> temp = new HashMap<>();
        for(Long id : fr_ref.keySet())
            temp.put(id,0d);
        this.splitmap = new CommandDoubleMap(temp);

        this.command = new HashMap<>();
        command.put(actid,splitmap);

    }

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {

        // get the flow that is currently entering linkin for this commodity
        double flow_in_vph = ml_sensor.get_flow_vph();

        // if the sensor registers no flow, then dont do anything.
        if(flow_in_vph<0.0001)
            return;

        for(Map.Entry<Long,Profile1D> e : fr_ref.entrySet()){
            Long frid = e.getKey();
            Profile1D prof = e.getValue();

            // get the desired exit flow
            double des_flow_out_vph = prof.get_value_for_time(dispatcher.current_time);

            // compute the split ratio
            double split = des_flow_out_vph / flow_in_vph;
            if(split<0d)
                split = 0d;
            if(split>1d)
                split = 1d;

            if(split>1)
                split=1d;

            splitmap.values.put(frid,split);
        }

    }
}
