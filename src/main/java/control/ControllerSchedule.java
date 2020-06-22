package control;

import actuator.AbstractActuator;
import common.Scenario;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import error.OTMException;
import jaxb.Controller;
import runner.ScenarioFactory;

import java.util.ArrayList;
import java.util.List;

public class ControllerSchedule extends AbstractController {

    protected List<ScheduleEntry> entries;
    private ScheduleEntry curr_entry;
    private int curr_entry_index;
    private float next_entry_start;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ControllerSchedule(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);

        // make sure this is not a periodic controller
        this.dt = 0;

        if(jaxb_controller.getSchedule()==null)
            return;

        entries = new ArrayList<>();

        for(jaxb.Entry e : jaxb_controller.getSchedule().getEntry()){
            float start_time = e.getStartTime();
            float end_time = e.getEndTime()==null ? Float.POSITIVE_INFINITY : e.getEndTime();
            jaxb.Controller jcntrl = new jaxb.Controller();
            if(e.getDt()!=null)
                jcntrl.setDt(e.getDt());
            jcntrl.setType(e.getType());
            jcntrl.setParameters(e.getParameters());
            jcntrl.setStartTime(start_time);
            jcntrl.setEndTime(end_time);
            AbstractController cntrl = ScenarioFactory.create_controller_from_jaxb(scenario,jcntrl);
            entries.add(new ScheduleEntry(start_time,end_time,cntrl));
        }

    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);

        curr_entry_index = -1;

        // assign actuator to entry controllers
        AbstractActuator act = this.actuators.values().iterator().next();
        for (ScheduleEntry entry : entries)
            entry.cntrl.actuators.put(act.id,act);

    }

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {

        System.out.println(String.format("%.2f\tupdate_command\t%s",scenario.dispatcher.current_time,this.getClass().getName()));

        float now = dispatcher.current_time;

        // possibly update the entry
        if(now>=next_entry_start)
            update_entry_index(now);

        if(curr_entry!=null)
            this.command = curr_entry.cntrl.command;

        // register next poke
        if(Float.isFinite(next_entry_start))
            dispatcher.register_event(new EventPoke(dispatcher,25,next_entry_start,this));

    }

    ///////////////////////////////////////////////////
    // private
    ///////////////////////////////////////////////////

    private void update_entry_index(float now) throws OTMException {

        int prev_entry_index = curr_entry_index;

        int next_index = entries.size();

        for (int i = 0; i < entries.size(); i++)
            if (entries.get(i).start_time > now) {
                next_index = i;
                break;
            }

        curr_entry_index = next_index - 1;
        next_entry_start = next_index<entries.size() ? entries.get(next_index).start_time : Float.POSITIVE_INFINITY;

        if(curr_entry_index<0){
            curr_entry = null;
            return;
        }

        // if the index has changed, update curr_entry, delete pending controller actions
        // and register current with dispatcher (initialize).
        if(curr_entry_index!=prev_entry_index){

            // delete pending controller actions
            if(curr_entry!=null)
                scenario.dispatcher.remove_events_for_recipient(EventPoke.class,curr_entry.cntrl);

            // assign
            curr_entry = entries.get(curr_entry_index);

            // initialize
            curr_entry.cntrl.initialize(scenario);
        }
    }

    ///////////////////////////////////////////////////
    // class
    ///////////////////////////////////////////////////

    public class ScheduleEntry {
        public float start_time;
        public float end_time;
        public AbstractController cntrl;
        public ScheduleEntry(float start_time,float end_time,AbstractController cntrl){
            this.start_time = start_time;
            this.end_time = end_time;
            this.cntrl = cntrl;
        }
    }

}
