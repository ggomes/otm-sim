package control.commodity;

import actuator.ActuatorFlowToLinks;
import core.Scenario;
import control.AbstractController;
import control.command.CommandDoubleArray;
import dispatch.Dispatcher;
import dispatch.EventPoke;
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
            int n = jcon.getProfiles().getProfile().size();
            fr_ids = new long[n];
            fr_prof = new Profile1D[n];
            for(int i=0;i<jcon.getProfiles().getProfile().size();i++){
                jaxb.Profile prof = jcon.getProfiles().getProfile().get(i);
                float prof_start_time = prof.getStartTime();
                fr_ids[i] = prof.getId();
                fr_prof[i] = prof_start_time>86400 ? null :
                        new Profile1D(prof_start_time,prof.getDt(), OTMUtils.csv2list(prof.getContent()));
            }
        }

        act = (ActuatorFlowToLinks) actuators.values().iterator().next();

        this.dt = -1f;
    }

    @Override
    public Class get_actuator_class() {
        return ActuatorFlowToLinks.class;
    }

    @Override
    public void validate_pre_init(OTMErrorLog errorLog) {
        super.validate_pre_init(errorLog);

        if(actuators.size()!=1)
            errorLog.addError("Offramp flow controller must have exactly one actuator.");

        for(int i=0;i<fr_prof.length;i++)
            if(fr_prof[i]!=null)
                fr_prof[i].validate_pre_init(errorLog);
    }

    @Override
    public void initialize(Scenario scenario,boolean override_targets) throws OTMException {
        super.initialize(scenario,override_targets);
        fr_command = new CommandDoubleArray(fr_ids);
        command.put(act.id,fr_command);
    }

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {
        
        float next_time = Float.POSITIVE_INFINITY;
        for(int i=0;i<fr_ids.length;i++){
            if(fr_prof[i]==null)
                continue;
            fr_command.values[i] = fr_prof[i].get_value_for_time(dispatcher.current_time) ;
            next_time = Math.min( next_time , fr_prof[i].get_next_update_time(dispatcher.current_time) );
        }

        if(Float.isFinite(next_time))
            dispatcher.register_event(new EventPoke(dispatcher,20,next_time,this));

    }

}
