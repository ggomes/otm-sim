package control.commodity;

import common.Scenario;
import control.AbstractController;
import control.command.CommandBoolean;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import error.OTMException;
import jaxb.Controller;
import utils.OTMUtils;

import java.util.*;

public class ControllerOpenCloseLaneGroup extends AbstractController {

    public List<OpenCloseCommand> ref;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ControllerOpenCloseLaneGroup(Scenario scenario, Controller jcnt) throws OTMException {
        super(scenario, jcnt);

        ref = new ArrayList<>();
        if(jcnt.getParameters()!=null && jcnt.getParameters().getParameter()!=null){
            for(jaxb.Parameter p : jcnt.getParameters().getParameter()){
                if(p.getName().compareToIgnoreCase("opentimes")==0){
                    for(double t :  OTMUtils.csv2list(p.getValue()))
                        ref.add(new OpenCloseCommand((float) t, true));
                }
                if(p.getName().compareToIgnoreCase("closetimes")==0) {
                    for (double t : OTMUtils.csv2list(p.getValue()))
                        ref.add(new OpenCloseCommand((float) t, false));
                }
            }
        }
        Collections.sort(ref);
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
    }

    public int get_index_for_time(float time){
        if(time<=ref.get(0).time)
            return 0;
        for(int index=1;index<ref.size();index++)
            if(time>=ref.get(index-1).time && time<ref.get(index).time)
                return index-1;
        return ref.size()-1;
    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {
        float timestamp = dispatcher.current_time;

        int curr_index = get_index_for_time(timestamp);
        CommandBoolean curr_command = new CommandBoolean(ref.get(curr_index).open);
        for (Long actid : command.keySet())
            command.put(actid, curr_command);

        // register next poke
        if(curr_index<ref.size()-1)
            dispatcher.register_event(new EventPoke(dispatcher,20,ref.get(curr_index+1).time,this));

    }

    class OpenCloseCommand implements Comparable<OpenCloseCommand>{
        float time;
        boolean open;
        public OpenCloseCommand(float time, boolean open) {
            this.time = time;
            this.open = open;
        }

        @Override
        public int compareTo(OpenCloseCommand that) {
            return Float.compare(this.time,that.time);
        }
    }
}
