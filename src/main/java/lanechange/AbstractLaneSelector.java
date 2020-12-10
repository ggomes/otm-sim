package lanechange;

import core.AbstractLaneGroup;
import core.Scenario;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import dispatch.Pokable;
import error.OTMException;
import core.State;
import models.Maneuver;
import models.fluid.AbstractFluidModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractLaneSelector implements Pokable {

    protected float dt;
    protected AbstractLaneGroup lg;
    protected Map<Long,Map<Maneuver,Double>> side2prob; // pathorlinkid-> maneuver -> probability
    protected long commid;

    // this will be called by a lgrestric controller whenever it changes the lc options.
    public abstract void update_lane_change_probabilities_with_options(Long pathorlinkid,Set<Maneuver> lcoptions);

    public AbstractLaneSelector(AbstractLaneGroup lg,float dt,long commid){
        this.dt = dt;
        this.lg = lg;
        this.commid = commid;
        side2prob = new HashMap<>();

        for(Map.Entry<State,Set<Maneuver>> e: lg.state2lanechangedirections.entrySet()){
            State s = e.getKey();
            if(s.commodity_id!=commid)
                continue;
            Map<Maneuver,Double> x = new HashMap<>();
            side2prob.put(s.pathOrlink_id,x);
            Set<Maneuver> maneuvers = e.getValue();
            double v = 1d/maneuvers.size();
            for(Maneuver m : maneuvers)
                x.put(m,v);
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

    public final void remove_maneuver(State state, Maneuver maneuver){
        if(side2prob.containsKey(state.pathOrlink_id) ) {
            Map<Maneuver,Double> e = side2prob.get(state.pathOrlink_id);
            if(e.containsKey(maneuver)){
                double prob = e.size()>1 ? e.get(maneuver)/(e.size()-1) : 0d;
                e.remove(maneuver);
                for(Maneuver m : e.keySet())
                    e.put( m,e.get(m)+prob);
            }
        }
    }

    public final void add_maneuver(State state, Maneuver maneuver){
        if(side2prob.containsKey(state.pathOrlink_id)){
            Map<Maneuver,Double> e = side2prob.get(state.pathOrlink_id);
            if(!e.containsKey(maneuver))
                e.put(maneuver,0d);
        } else {
            Map<Maneuver,Double> e = new HashMap<>();
            side2prob.put(state.pathOrlink_id,e);
            e.put(maneuver,0d);
        }
    }

    public final Map<Maneuver,Double> get_lanechange_probabilities(Long pathorlinkid){
        return side2prob.get(pathorlinkid);
    }

    public final void update_lane_change_probabilities() {
        for(Long pathorlinkid : side2prob.keySet())
            update_lane_change_probabilities_with_options(pathorlinkid,side2prob.get(pathorlinkid).keySet());
    }
}
