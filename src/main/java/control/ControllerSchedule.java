package control;

import actuator.AbstractActuator;
import core.Scenario;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Controller;
import core.ScenarioFactory;

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

        AbstractActuator act = this.actuators.values().iterator().next();

        // make sure this is not a periodic controller
        this.dt = 0;

        if(jaxb_controller.getSchedule()==null)
            return;

        entries = new ArrayList<>();

        for(jaxb.Entry e : jaxb_controller.getSchedule().getEntry()){
            float start_time = e.getStartTime()==null ? 0f : e.getStartTime();
            float end_time = e.getEndTime()==null ? Float.POSITIVE_INFINITY : e.getEndTime();
            jaxb.Controller jcntrl = new jaxb.Controller();
            if(e.getDt()!=null)
                jcntrl.setDt(e.getDt());
            jcntrl.setType(e.getType());
            jcntrl.setParameters(e.getParameters());
            jcntrl.setStartTime(start_time);
            jcntrl.setEndTime(end_time);
            jcntrl.setFeedbackSensors(e.getFeedbackSensors());
            AbstractController cntrl = ScenarioFactory.create_controller_from_jaxb(scenario,jcntrl);


            ScheduleEntry entry = new ScheduleEntry(start_time,end_time,cntrl);
            entries.add(entry);

            cntrl.actuators.put(act.id,act);
        }

    }

    @Override
    public Class get_actuator_class() {
        return AbstractActuator.class;
    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);
        for(ScheduleEntry entry : entries)
            entry.validate(errorLog);
    }

    @Override
    public void initialize(Scenario scenario,boolean override_targets) throws OTMException {
        super.initialize(scenario,override_targets);

        curr_entry_index = -1;

        // disconnect actuators
        for(AbstractActuator act : actuators.values())
            act.myController = null;

        // assign actuator to entry controllers
        for (ScheduleEntry entry : entries)
            for(AbstractActuator act : actuators.values())
                entry.cntrl.actuators.put(act.id,act);

    }

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {

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
            curr_entry.cntrl.initialize(scenario,true);
            curr_entry.cntrl.poke(scenario.dispatcher,now);
        }
    }

    ///////////////////////////////////////////////////
    // class
    ///////////////////////////////////////////////////

    public class ScheduleEntry {
        public float start_time;
        public float end_time;
        public AbstractController cntrl;

        public ScheduleEntry(float start_time, float end_time, AbstractController cntrl) {
            this.start_time = start_time;
            this.end_time = end_time;
            this.cntrl = cntrl;
        }

        public void initialize(Scenario scenario) throws OTMException {
            cntrl.initialize(scenario,true);
        }

        public void validate(OTMErrorLog errorLog) {
            cntrl.validate(errorLog);
        }
    }
}
