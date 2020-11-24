package control.commodity;

import actuator.ActuatorFlowToLinks;
import common.Scenario;
import control.AbstractController;
import control.command.CommandDoubleArray;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Controller;
import profiles.Profile1D;
import utils.OTMUtils;

public class ControllerFlowToLinks extends AbstractController  {

    private long[] fr_ids;
    private Profile1D[] fr_prof;
    private CommandDoubleArray fr_command;
    private ActuatorFlowToLinks act;

    public ControllerFlowToLinks(Scenario scenario, Controller jcon) throws OTMException {
        super(scenario, jcon);

        if(jcon.getProfiles()!=null){
            int i = 0;
            int n = jcon.getProfiles().getProfile().size();
            fr_ids = new long[n];
            fr_prof = new Profile1D[n];
            for(jaxb.Profile prof : jcon.getProfiles().getProfile()){
                float prof_start_time = prof.getStartTime();
                fr_ids[i] = prof.getId();
                fr_prof[i] = prof_start_time>86400 ?
                        null :
                        new Profile1D(prof_start_time,prof.getDt(), OTMUtils.csv2list(prof.getContent()));
            }
        }

        act = (ActuatorFlowToLinks) actuators.values().iterator().next();
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);

        if(actuators.size()!=1)
            errorLog.addError("Offramp flow controller must have exactly one actuator.");

        for(int i=0;i<fr_prof.length;i++)
            if(fr_prof[i]!=null)
                fr_prof[i].validate(errorLog);

//        Set<Long> act_linkids = act.linkFRs.stream().map(l->l.getId()).collect(Collectors.toSet());
//        if(!act_linkids.equals(fr_ids))
//            errorLog.addError("Controller and actuator link ids dont match.");
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
        fr_command = new CommandDoubleArray(fr_ids);
        command.put(act.id,fr_command);
    }

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {
        for(int i=0;i<fr_ids.length;i++){
            Profile1D prof = fr_prof[i];
            if(prof==null)
                continue;
            fr_command.values[i] = prof.get_value_for_time(dispatcher.current_time) ;
        }
    }

}
