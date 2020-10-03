package lanechange;

import common.AbstractLaneGroup;
import common.Scenario;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import dispatch.Pokable;
import error.OTMException;
import geometry.Side;
import keys.State;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractLaneSelector implements Pokable {

    protected float dt;
    protected AbstractLaneGroup lg;
    protected Map<State,Map<Side,Double>> side2prob; // state->side -> probability

    // this will be called by a lgrestric controller whenever it changes the lc options.
    public abstract void update_lane_change_probabilities_with_options(State state,Set<Side> lcoptions);

    public AbstractLaneSelector(AbstractLaneGroup lg,float dt){
        this.dt = dt;
        this.lg = lg;
        side2prob = new HashMap<>();

        for(Map.Entry<State,Set<Side>> e: lg.state2lanechangedirections.entrySet()){
            State s = e.getKey();
            Map<Side,Double> x = new HashMap<>();
            side2prob.put(s,x);
            Set<Side> sides = e.getValue();
            double v = 1d/sides.size();
            for(Side side : sides)
                x.put(side,v);
        }

    }

    public void initialize(Scenario scenario) throws OTMException {
        poke(scenario.dispatcher,scenario.dispatcher.current_time);
    }

    @Override
    public void poke(Dispatcher dispatcher, float timestamp) throws OTMException {
        update_lane_change_probabilities();
        if(dt>0)
            dispatcher.register_event(new EventPoke(dispatcher,5,timestamp+dt,this));
    }

    public final Map<Side,Double> get_lanechange_probabilities(State state){
        return side2prob.get(state);
    }

    public final void update_lane_change_probabilities() {
        for(State state : side2prob.keySet())
            update_lane_change_probabilities_with_options(state,side2prob.get(state).keySet());
    }
}
