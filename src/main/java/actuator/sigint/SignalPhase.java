/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package actuator.sigint;

import actuator.AbstractActuator;
import common.RoadConnection;
import dispatch.Dispatcher;
import dispatch.EventAdvanceSignalPhase;
import error.OTMErrorLog;
import error.OTMException;
import runner.Scenario;
import runner.ScenarioElementType;
import utils.OTMUtils;
import utils.CircularList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class SignalPhase {

    public final long id;
    public ActuatorSignal my_signal;
    public CircularList<PhaseTransition> transitions;
    public float yellow_time;
    public float red_clear_time;
    public float min_green_time;

    public Set<RoadConnection> road_connections;

    // state: bulb color and transition pointer
    public BulbColor bulbcolor;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public SignalPhase(Scenario scenario, AbstractActuator actuator, jaxb.Phase jaxb_phase) throws OTMException {
        this.id = jaxb_phase.getId();
        this.my_signal = (ActuatorSignal) actuator;
        this.yellow_time = jaxb_phase.getYellowTime();
        this.red_clear_time = jaxb_phase.getRedClearTime();
        this.min_green_time = jaxb_phase.getMinGreenTime();
        this.transitions = new CircularList<>();
        road_connections = new HashSet<>();
        for(Long id : OTMUtils.csv2longlist(jaxb_phase.getRoadconnectionIds())) {
            RoadConnection rc = (RoadConnection) scenario.get_element(ScenarioElementType.roadconnection, id);
            if(rc==null)
                throw new OTMException("bad road connection id in actuator id=" + this.id);
            road_connections.add(rc);
        }
    }

    public void validate(OTMErrorLog errorLog) {

        // positivity
        if(yellow_time<0)
            errorLog.addError("yellow_time<0");

        if(red_clear_time<0)
            errorLog.addError("red_clear_time<0");

        if(min_green_time<0)
            errorLog.addError("min_green_time<0");

        // valid road connections
        if(road_connections.isEmpty())
            errorLog.addError("road_connections.isEmpty()");

        if(road_connections.contains(null))
            errorLog.addError("road_connections.contains(null)");

    }

    public void initialize(float now) throws OTMException {
        set_bulb_color(now,BulbColor.DARK);
    }

    ///////////////////////////////////////////////////
    // interface
    ///////////////////////////////////////////////////

    // when called, the phase has only green->red and red->green transitions.
    // this method removes simultaneous transitions of this type.
    // it is used for phases that span multiple stages.
    public void cancel_redundate_transitions(){

        // remove zero time superfluous transitions
        Set<Float> unique_times = new HashSet<>();
        for(PhaseTransition p : transitions.queue)
            unique_times.add(p.cycle_time);
        for(Float t : unique_times){
            // find all transitions with this time
            List<PhaseTransition> trans = transitions.queue.stream().filter(x->x.cycle_time==t).collect(toList());
            if(trans.size()>2)
                System.err.println("I DONT KNOW WHAT TO DO HERE!");
            if(trans.size()==2 && PhaseTransition.are_equal(trans.get(0),trans.get(1)))
                transitions.remove(trans);
        }
    }

    // replaces green->red transitions with green->yellow and yellow->red
    public void insert_yellow_time(){

        // extract green->red transitions
        Set<PhaseTransition> end_transitions = transitions.queue.stream()
                .filter(x->x.from_color==BulbColor.GREEN && x.to_color==BulbColor.RED)
                .collect(Collectors.toSet());

        for(PhaseTransition p : end_transitions){
            float end_time = p.cycle_time;
            float g2y = (my_signal.cycle()+end_time-red_clear_time-yellow_time)%my_signal.cycle();
            float y2r = (g2y+yellow_time)%my_signal.cycle();
            transitions.add(new PhaseTransition(g2y,BulbColor.GREEN,BulbColor.YELLOW));
            transitions.add(new PhaseTransition(y2r,BulbColor.YELLOW,BulbColor.RED));
            transitions.remove(p);
        }
    }

    public void turn_off(float timestamp) throws OTMException {
        set_bulb_color(timestamp, BulbColor.DARK);
    }

    public void print_transitions(float timestamp){
        for(PhaseTransition pt : transitions.queue)
            System.out.println(String.format("%.1f\t%d\t%d\t%s",timestamp,my_signal.id,id,pt.toString()));
    }

    ///////////////////////////////////////////////////
    // update
    ///////////////////////////////////////////////////

    public void advance_transitions(float timestamp) throws OTMException {

        // advance the pointer to now
        transitions.step_forward();

        // get the transition and set the bulb color
        set_bulb_color(timestamp,transitions.peek().to_color);
    }

    public void register_next_transtions(Dispatcher dispatcher,float timestamp,float cycle_time){

        // register next transition
        PhaseTransition next_trans = transitions.peek_next();
        float next_trans_cycle_time = next_trans.cycle_time<cycle_time ?
                next_trans.cycle_time+my_signal.cycle() :
                next_trans.cycle_time;

        float next_trans_abs_time = next_trans_cycle_time + timestamp - cycle_time;

        dispatcher.register_event(new EventAdvanceSignalPhase(dispatcher,next_trans_abs_time,this));
    }

    public void execute_next_transition_and_register_following(Dispatcher dispatcher,float timestamp) throws OTMException {

        if(transitions.queue.isEmpty())
            return;

        // advance the pointer to now
        transitions.step_forward();

        // get the transitiion and set the bulb color
        PhaseTransition now_trans = transitions.peek();
        set_bulb_color(timestamp,now_trans.to_color);

        // register next transition
        PhaseTransition next_trans = transitions.peek_next();
        float next_trans_cycle_time = next_trans.cycle_time<now_trans.cycle_time ?
                next_trans.cycle_time+my_signal.cycle() :
                next_trans.cycle_time;

        float next_trans_abs_time = next_trans_cycle_time + timestamp - now_trans.cycle_time;

        dispatcher.register_event(new EventAdvanceSignalPhase(dispatcher,next_trans_abs_time,this));

    }

    ///////////////////////////////////////////////////
    // protected
    ///////////////////////////////////////////////////

    protected void set_bulb_color(float timestamp,BulbColor to_color) throws OTMException {

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
        for(RoadConnection rc : road_connections)
            rc.set_external_max_flow_vps(rate_vps);

        // inform the output listener
        if(my_signal.event_listener!=null)
            my_signal.event_listener.write(timestamp,new api.events.EventSignalPhase(timestamp,id,bulbcolor));

    }

    protected PhaseTransition get_transition_preceding(float cycle_time){

        int n = transitions.queue.size();

        if(n==0)
            return null;

        // time before first transition
        if(cycle_time<=transitions.get(0).cycle_time)
            return transitions.get(n-1);

        // time within transitions
        for(int e=1;e<n;e++)
            if(cycle_time<=transitions.get(e).cycle_time)
                return transitions.get(e-1);

        // time after last transition
        return transitions.get(n-1);
    }

}
