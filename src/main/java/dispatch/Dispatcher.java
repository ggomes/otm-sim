/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package dispatch;

import error.OTMException;
import runner.RunParameters;
import runner.Scenario;

import java.util.*;

import static java.util.stream.Collectors.toSet;

public  class Dispatcher {

    public Scenario scenario;
    public float current_time;
    public float start_time;
    public float stop_time;
    public PriorityQueue<AbstractEvent> events;
    private boolean continue_simulation;

    // references to vehicle release events,
    // used by disable_future_vehicle_release_events
//    public Map<Long,Set<EventReleaseVehicleFromLaneGroup>> vehicle_release_events;

    public boolean verbose = false;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public Dispatcher(float start_time){
        this.start_time = start_time;
        this.events = new PriorityQueue<>((AbstractEvent e1, AbstractEvent e2) ->
                    e1.timestamp>e2.timestamp ? 1 : -1 ) ;
        this.continue_simulation = false;
    }

    public void set_stop_time(float stop_time){
        this.stop_time = stop_time;
    }

    public void set_scenario(Scenario scenario){
        this.scenario = scenario;
    }

    public void initialize(float current_time) throws OTMException {
        this.current_time = current_time;
        this.events.clear();
        this.continue_simulation = true;
    }

    public void set_continue_simulation(boolean x){
        this.continue_simulation = x;
    }

    ///////////////////////////////////////////////////
    // update
    ///////////////////////////////////////////////////

    public void remove_events_for_recipient(Class<? extends AbstractEvent> clazz){

        Set<AbstractEvent> remove = events.stream()
                .filter(x-> x.getClass()==clazz)
                .collect(toSet());

        events.removeAll(remove);
    }

    public void remove_events_for_recipient(Class<? extends AbstractEvent> clazz, Object recipient){

        Set<AbstractEvent> remove = events.stream()
                .filter(x->x.recipient==recipient && x.getClass()==clazz)
                .collect(toSet());

        events.removeAll(remove);
    }

    public void register_event(AbstractEvent event){
        if(event.timestamp<current_time) // || event.timestamp>end_time)
            return;
        events.offer(event);
    }

    public void dispatch_events_to_stop() throws OTMException {

        while( !events.isEmpty() && continue_simulation ) {
            float timestamp = events.peek().timestamp;
            current_time = timestamp;

            // get all events with this timestamp
            // put into priority queue ordered by dispatch order
            PriorityQueue<AbstractEvent> es = new PriorityQueue<>(
                    (AbstractEvent e1, AbstractEvent e2) -> e1.dispatch_order>e2.dispatch_order ? 1 : -1 ) ;
            while(!events.isEmpty() && events.peek().timestamp==timestamp)
                es.offer(events.poll());

            // dispatch the priority queue
            while(!es.isEmpty())
                es.poll().action(verbose);
        }
    }

    public void stop(){
        continue_simulation = false;
    }

    public void print_events(){
        this.events.stream().forEach(x->System.out.println(x.toString()));
    }

}
