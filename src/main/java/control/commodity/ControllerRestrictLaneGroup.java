package control.commodity;

import actuator.ActuatorLaneGroupAllowComm;
import core.Scenario;
import control.AbstractController;
import control.command.CommandRestrictionMap;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import error.OTMException;
import jaxb.Controller;
import utils.OTMUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ControllerRestrictLaneGroup extends AbstractController {

    public enum Restriction {Open,Closed}

    public Set<Long> free_comms = new HashSet<>();
    public Set<Long> banned_comms = new HashSet<>();

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ControllerRestrictLaneGroup(Scenario scenario, Controller jcnt) throws OTMException {
        super(scenario, jcnt);
        this.dt=0f;

        if(jcnt.getParameters()!=null)
            for(jaxb.Parameter p : jcnt.getParameters().getParameter()){
                switch(p.getName()){
                    case "free_comms":
                        free_comms.addAll(OTMUtils.csv2longlist(p.getValue()));
                        break;
                    case "disallowed_comms":
                        banned_comms.addAll(OTMUtils.csv2longlist(p.getValue()));
                        break;
                }
            }
    }

    @Override
    public Class get_actuator_class() {
        return ActuatorLaneGroupAllowComm.class;
    }

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {
        float timestamp = dispatcher.current_time;

        Map<Long,Restriction> X = new HashMap<>();
        if (timestamp < end_time) {
            for (Long commid : free_comms)
                X.put(commid, Restriction.Open);
            for (Long commid : banned_comms)
                X.put(commid, Restriction.Closed);
        }
        else{
            for(Long commid : this.scenario.commodities.keySet())
                X.put(commid,Restriction.Open);
        }

        long act_id = this.actuators.keySet().iterator().next();
        command.put(act_id,new CommandRestrictionMap(X));

        // register next poke
        if (timestamp < end_time)
            dispatcher.register_event(new EventPoke(dispatcher,19,this.end_time,this));

    }

}
