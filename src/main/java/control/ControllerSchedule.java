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
//            jcntrl.setTargetActuators(jaxb_controller.getTargetActuators());
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
        update_entry_index(scenario.get_current_time());
        AbstractActuator act = this.actuators.values().iterator().next();
        for (ScheduleEntry entry : entries) {
            entry.cntrl.actuators.put(act.id,act);
            entry.cntrl.initialize(scenario);
        }
    }

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {

        float now = dispatcher.current_time;
        System.out.println(now + "\tControllerSchedule " + id + " update_command");

        // possibly update the entry
        if(now>=next_entry_start)
            update_entry_index(now);

        if(curr_entry_index>=0){
            ScheduleEntry curr_entry = entries.get(curr_entry_index);

            // get command for the current entry and record it to command
            AbstractController ctrl = curr_entry.cntrl;
            ctrl.update_command(dispatcher);
            this.command = ctrl.command;
        }

        // register next poke
        if(Float.isFinite(next_entry_start))
            dispatcher.register_event(new EventPoke(dispatcher,2,next_entry_start,this));

    }

    ///////////////////////////////////////////////////
    // private
    ///////////////////////////////////////////////////

    private void update_entry_index(float now) {

        int next_index = entries.size();

        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).start_time > now) {
                next_index = i;
                break;
            }
        }

        curr_entry_index = next_index - 1;
        next_entry_start = next_index<entries.size() ? entries.get(next_index).start_time : Float.POSITIVE_INFINITY;

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
