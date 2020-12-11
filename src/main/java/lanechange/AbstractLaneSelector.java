package lanechange;

import common.AbstractLaneGroup;
import common.Scenario;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import dispatch.Pokable;
import error.OTMException;
import geometry.Side;
import keys.State;
import models.fluid.AbstractFluidModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractLaneSelector implements Pokable {

    protected float dt;
    protected AbstractLaneGroup lg;
    protected Map<Long,Map<Side,Double>> side2prob; // pathorlinkid->side -> probability
    protected long commid;

    // this will be called by a lgrestric controller whenever it changes the lc options.
    public abstract void update_lane_change_probabilities_with_options(Long pathorlinkid,Set<Side> lcoptions);

    public AbstractLaneSelector(AbstractLaneGroup lg,float dt,long commid){
        this.dt = dt;
        this.lg = lg;
        this.commid = commid;
        side2prob = new HashMap<>();

        for(Map.Entry<State,Set<Side>> e: lg.state2lanechangedirections.entrySet()){
            State s = e.getKey();
            if(s.commodity_id!=commid)
                continue;
            Map<Side,Double> x = new HashMap<>();
            side2prob.put(s.pathOrlink_id,x);
            Set<Side> sides = e.getValue();
            double v = 1d/sides.size();
            for(Side side : sides)
                x.put(side,v);
        }


        // dt==0 means update every time step
        // dt<0 means update only once upon initialization
        if(this.dt==0 && (lg.link.model instanceof  AbstractFluidModel))
            this.dt = ((AbstractFluidModel)lg.link.model).dt_sec;

    }

    public void initialize(Scenario scenario, float start_time) throws OTMException {
        poke(scenario.dispatcher,start_time);
    }

    @Override
    public void poke(Dispatcher dispatcher, float timestamp) throws OTMException {
        update_lane_change_probabilities();
        if(dt>0)
            dispatcher.register_event(new EventPoke(dispatcher,5,timestamp+dt,this));
    }

    public final void remove_side(State state,Side side){
        if(side2prob.containsKey(state.pathOrlink_id) ) {
            Map<Side,Double> e = side2prob.get(state.pathOrlink_id);
            if(e.containsKey(side)){
                double prob = e.size()>1 ? e.get(side)/(e.size()-1) : 0d;
                e.remove(side);
                for(Side s : e.keySet())
                    e.put(s,e.get(s)+prob);
            }
        }
    }

    public final void add_side(State state,Side side){
        if(side2prob.containsKey(state.pathOrlink_id)){
            Map<Side,Double> e = side2prob.get(state.pathOrlink_id);
            if(!e.containsKey(side))
                e.put(side,0d);
        } else {
            Map<Side,Double> e = new HashMap<>();
            side2prob.put(state.pathOrlink_id,e);
            e.put(side,0d);
        }
    }

    public final Map<Side,Double> get_lanechange_probabilities(Long pathorlinkid){
        return side2prob.get(pathorlinkid);
    }

    public final void update_lane_change_probabilities() {
        for(Long pathorlinkid : side2prob.keySet())
            update_lane_change_probabilities_with_options(pathorlinkid,side2prob.get(pathorlinkid).keySet());
    }
}
