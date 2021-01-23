package control.commodity;

import actuator.ActuatorFlowToLinks;
import control.command.CommandLongToDouble;
import core.AbstractFluidModel;
import core.Link;
import core.Scenario;
import control.AbstractController;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Controller;
import profiles.Profile1D;
import utils.OTMUtils;

import java.util.*;
import java.util.stream.Collectors;


/**
 * This controller is used to specify the flow that goes to a branch in a splitting node as absolute values instead of
 * split ratios.
 * Example:
 *  <controller id="1" type="linkflow" start_time="0.0" dt="300">
 *      <target_actuators ids="1"/>
 *      <profiles>
 *          <profile id="4">100,200</profile>
 *          <profile id="5">100,300</profile>
 *      </profiles>
 *  </controller>
 *
 * Coupled with this actuator:
 * <actuator id="1" type="flowtolink" passive="true">
 *     <actuator_target type="link" id="2" commids="0"/>
 *     <parameters>
 *         <parameter name="rcid" value="1" />
 *     </parameters>
 * </actuator>
 *
 * The actuator enables the control of flow of commodity 0 from link 2 to links 4 and 5. The controller defines profiles
 * of flow to be sent, in units veh/hr. The start times and dt's of the profiles all equal the start time and dt of the
 * controller. Each link (linkin) can have at most one actuator of this type per commodity.
 */
public class ControllerFlowToLinks extends AbstractController  {

    public Map<Long,Profile1D> outlink2profile;  // map of outlinks to profiles.
    private ActuatorFlowToLinks act;

    public ControllerFlowToLinks(Scenario scenario, Controller jcon) throws OTMException {
        super(scenario, jcon);

        Float prof_dt = dt;
        if(jcon.getProfiles()!=null){
            outlink2profile = new HashMap<>();
            for(int i=0;i<jcon.getProfiles().getProfile().size();i++){
                jaxb.Profile prof = jcon.getProfiles().getProfile().get(i);
                outlink2profile.put(prof.getId(),new Profile1D(start_time,prof_dt, OTMUtils.csv2list(prof.getContent())) );
            }
        }

        act = (ActuatorFlowToLinks) actuators.values().iterator().next();
        dt = null;
   }

    @Override
    public Class get_actuator_class() {
        return ActuatorFlowToLinks.class;
    }

    @Override
    protected void configure() throws OTMException {
        command.put(act.id,new CommandLongToDouble() );
    }

    @Override
    public void validate_pre_init(OTMErrorLog errorLog) {
        super.validate_pre_init(errorLog);

        if(actuators.size()!=1)
            errorLog.addError("Offramp flow controller must have exactly one actuator.");
        for(Profile1D prof : outlink2profile.values())
            prof.validate_pre_init(errorLog);
    }

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {

        if(outlink2profile.size()==0)
            return;

        float next_time = Float.POSITIVE_INFINITY;
        CommandLongToDouble cmnd = (CommandLongToDouble) command.get(act.id);
        float now = dispatcher.current_time;

        Profile1D aprof = outlink2profile.values().iterator().next();
        int index = aprof.get_index_for_time(now);
        next_time = Math.min( next_time , aprof.get_next_update_time(now) );

        for(Map.Entry<Long,Profile1D> e : outlink2profile.entrySet()){
            Long outlinkid = e.getKey();
            Profile1D prof = e.getValue();
            cmnd.X.put(outlinkid, prof.get_ith_value(index)) ;
        }

        if(Float.isFinite(next_time))
            dispatcher.register_event(new EventPoke(dispatcher,20,next_time,this));

    }

}
