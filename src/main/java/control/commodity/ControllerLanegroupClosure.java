package control.commodity;

import common.Scenario;
import control.AbstractController;
import control.command.CommandOpenClosed;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import error.OTMException;
import jaxb.Controller;
import utils.OTMUtils;

import java.util.HashSet;
import java.util.Set;

public class ControllerLanegroupClosure extends AbstractController {

    public Set<Long> commids;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ControllerLanegroupClosure(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);
        if(jaxb_controller.getParameters()!=null && jaxb_controller.getParameters().getParameter()!=null){
            for(jaxb.Parameter p : jaxb_controller.getParameters().getParameter()){
                if(p.getName().compareToIgnoreCase("comms")==0) {
                    this.commids = new HashSet<>();
                    this.commids.addAll( OTMUtils.csv2longlist(p.getValue()) );
                }
            }
        }
    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
    }

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {
        float timestamp = dispatcher.current_time;

        if(timestamp<start_time) {
            for (Long actid : command.keySet())
                command.put(actid, CommandOpenClosed.open);
            dispatcher.register_event(new EventPoke(dispatcher,20,start_time,this));
        }

        if(timestamp>=start_time && timestamp<end_time) {
            for (Long actid : command.keySet())
                command.put(actid, CommandOpenClosed.closed);
            dispatcher.register_event(new EventPoke(dispatcher,20,end_time,this));
        }

        if(timestamp>=end_time)
            for(Long actid : command.keySet())
                command.put(actid, CommandOpenClosed.open);

    }

}
