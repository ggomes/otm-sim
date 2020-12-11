package actuator;

import output.events.EventSignalPhaseInfo;
import common.AbstractLaneGroup;
import common.RoadConnection;
import error.OTMErrorLog;
import error.OTMException;
import common.Scenario;
import common.ScenarioElementType;
import utils.OTMUtils;

import java.util.HashSet;
import java.util.Set;

public class SignalPhase {

    public enum BulbColor {
        GREEN,YELLOW,RED,DARK
    }

    public final long id;
    public ActuatorSignal my_signal;
//    public CircularList<PhaseTransition> transitions;
//    public float yellow_time;
//    public float red_clear_time;
//    public float min_green_time;

    public Set<Long> rc_ids;
    public Set<AbstractLaneGroup> lanegroups;

    // state: bulb color and transition pointer
    public BulbColor bulbcolor;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public SignalPhase(Scenario scenario, AbstractActuator actuator, jaxb.Phase jaxb_phase) throws OTMException {
        this.id = jaxb_phase.getId();
        this.my_signal = (ActuatorSignal) actuator;
//        this.yellow_time = jaxb_phase.getYellowTime();
//        this.red_clear_time = jaxb_phase.getRedClearTime();
//        this.min_green_time = jaxb_phase.getMinGreenTime();
//        this.transitions = new CircularList<>();


//        road_connections = new HashSet<>();
//        for(Long id : OTMUtils.csv2longlist(jaxb_phase.getRoadconnectionIds())) {
//            RoadConnection rc = (RoadConnection) scenario.get_element(ScenarioElementType.roadconnection, id);
//            if(rc==null)
//                throw new OTMException("bad road connection id in actuator id=" + this.id);
//            road_connections.add(rc);
//        }

        rc_ids = new HashSet<>();
        lanegroups = new HashSet<>();
        for(Long id : OTMUtils.csv2longlist(jaxb_phase.getRoadconnectionIds())) {
            RoadConnection rc = (RoadConnection) scenario.get_element(ScenarioElementType.roadconnection, id);
            if(rc==null)
                throw new OTMException("bad road connection id in actuator id=" + this.id);
            rc_ids.add(id);
            lanegroups.addAll(rc.in_lanegroups);
        }
    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement-like
    ///////////////////////////////////////////////////

    public void validate(OTMErrorLog errorLog) {

//        // positivity
//        if(yellow_time<0)
//            errorLog.addError("yellow_time<0");
//
//        if(red_clear_time<0)
//            errorLog.addError("red_clear_time<0");
//
//        if(min_green_time<0)
//            errorLog.addError("min_green_time<0");

        // valid road connections
        if(lanegroups.isEmpty())
            errorLog.addError("lanegroups.isEmpty()");
    }

    public void initialize() throws OTMException {
        for(AbstractLaneGroup lg : lanegroups)
            lg.register_actuator(null,my_signal);
        set_bulb_color(BulbColor.DARK);
    }

    ///////////////////////////////////////////////////
    // ActuatorSignal.process_controller_command
    ///////////////////////////////////////////////////

    protected void set_bulb_color(BulbColor to_color) throws OTMException {

        // set the state
        bulbcolor = to_color;

        // compute control rate
        Float rate_vps = Float.NaN;
        switch(to_color){
            case RED:
            case YELLOW:
            case DARK:
                rate_vps = 0f;
                break;
            case GREEN:
                rate_vps = Float.POSITIVE_INFINITY;
                break;
        }

        // record the queue at beginning of green
        //if(to_color==GREEN)
        //for(AbstractLaneGroup lanegroup : lanegroups)
        //    lanegroup_vehiclesAtToGreen.put(lanegroup.id, lanegroup.get_total_vehicles());

        // send to lane groups
        for(AbstractLaneGroup lg : lanegroups)
            lg.set_actuator_capacity_vps(rate_vps);

//        // inform the output listener
//        if(my_signal.event_output !=null)
//            my_signal.event_output.write(timestamp,new EventSignalPhaseInfo(timestamp,id,bulbcolor));

    }


    ///////////////////////////////////////////////////
    // interface
    ///////////////////////////////////////////////////

//    // when called, the phase has only green->red and red->green transitions.
//    // this method removes simultaneous transitions of this type.
//    // it is used for phases that span multiple stages.
//    public void cancel_redundate_transitions(){
//
//        // remove zero time superfluous transitions
//        Set<Float> unique_times = new HashSet<>();
//        for(PhaseTransition p : transitions.queue)
//            unique_times.add(p.cycle_time);
//        for(Float t : unique_times){
//            // find all transitions with this time
//            List<PhaseTransition> trans = transitions.queue.stream().filter(x->x.cycle_time==t).collect(toList());
//            if(trans.size()>2)
//                System.err.println("I DONT KNOW WHAT TO DO HERE!");
//            if(trans.size()==2 && PhaseTransition.are_equal(trans.get(0),trans.get(1)))
//                transitions.remove(trans);
//        }
//    }

    ///////////////////////////////////////////////////
    // update
    ///////////////////////////////////////////////////

//    public void advance_transitions(float timestamp) throws OTMException {
//
//        // advance the pointer to now
//        transitions.step_forward();
//
//        // get the transition and set the bulb color
//        set_bulb_color(timestamp,transitions.peek().to_color);
//    }

//    public void register_next_transtions(Dispatcher dispatcher,float timestamp,float cycle_time){
//
//        // register next transition
//        PhaseTransition next_trans = transitions.peek_next();
//        float next_trans_cycle_time = next_trans.cycle_time<cycle_time ?
//                next_trans.cycle_time+my_signal.cycle() :
//                next_trans.cycle_time;
//
//        float next_trans_abs_time = next_trans_cycle_time + timestamp - cycle_time;
//
//        dispatcher.register_event(new EventAdvanceSignalPhase(dispatcher,next_trans_abs_time,this));
//    }

//    public void execute_next_transition_and_register_following(Dispatcher dispatcher,float timestamp) throws OTMException {
//
//        if(transitions.queue.isEmpty())
//            return;
//
//        // advance the pointer to now
//        transitions.step_forward();
//
//        // get the transitiion and set the bulb color
//        PhaseTransition now_trans = transitions.peek();
//        set_bulb_color(timestamp,now_trans.to_color);
//
//        // register next transition
//        PhaseTransition next_trans = transitions.peek_next();
//        float next_trans_cycle_time = next_trans.cycle_time<now_trans.cycle_time ?
//                next_trans.cycle_time+my_signal.cycle() :
//                next_trans.cycle_time;
//
//        float next_trans_abs_time = next_trans_cycle_time + timestamp - now_trans.cycle_time;
//
//        dispatcher.register_event(new EventAdvanceSignalPhase(dispatcher,next_trans_abs_time,this));
//
//    }

    ///////////////////////////////////////////////////
    // protected
    ///////////////////////////////////////////////////

//    protected PhaseTransition get_transition_preceding(float cycle_time){
//
//        int n = transitions.queue.size();
//
//        if(n==0)
//            return null;
//
//        // time before first transition
//        if(cycle_time<=transitions.get(0).cycle_time)
//            return transitions.get(n-1);
//
//        // time within transitions
//        for(int e=1;e<n;e++)
//            if(cycle_time<=transitions.get(e).cycle_time)
//                return transitions.get(e-1);
//
//        // time after last transition
//        return transitions.get(n-1);
//    }

}
