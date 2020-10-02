package lanechange;

import common.AbstractLaneGroup;
import common.Scenario;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import dispatch.Pokable;
import error.OTMException;
import geometry.Side;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractLaneSelector implements Pokable {

    protected float dt;
    protected AbstractLaneGroup lg;
    protected Map<Side,Double> side2prob; // side -> probability

    // this will be called by a lgrestric controller whenever it changes the lc options.
    public abstract void update_lane_change_probabilities_with_options(Set<Side> lcoptions);

    public AbstractLaneSelector(AbstractLaneGroup lg,float dt){
        this.dt = dt;
        this.lg = lg;
        side2prob = new HashMap<>();
        side2prob.put(Side.middle,null);
        if(lg.neighbor_in!=null)
            side2prob.put(Side.in,null);
        if(lg.neighbor_out!=null)
            side2prob.put(Side.out,null);
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

    public final Map<Side,Double> get_lanechange_probabilities(){
        return side2prob;
    }

    public final void update_lane_change_probabilities() {
        update_lane_change_probabilities_with_options(side2prob.keySet());
    }
}
