package lanechange;

import core.AbstractLaneGroup;
import core.Link;
import core.Scenario;
import core.State;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import dispatch.Pokable;
import error.OTMException;
import models.fluid.AbstractFluidModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LinkLaneSelector implements Pokable {

    protected float dt;
    protected Link  link;
    public Map<Long, Map<State, InterfaceLaneSelector>> lcs;     // lg id -> state -> algorithm

    public LinkLaneSelector(String type, float dt, jaxb.Parameters params, Link link, Collection<Long> commids) throws OTMException {
        this.dt = dt;
        this.link = link;

        lcs = new HashMap<>();
        for(AbstractLaneGroup lg : link.lgs ) {
            Map<State, InterfaceLaneSelector> lcslg = new HashMap<>();
            lcs.put(lg.id, lcslg);
            for (State state : lg.states) {
                if (commids.contains(state.commodity_id))
                    lcslg.put(state, create_lane_selector(lg, type, params));
            }
        }

        // dt==0 means update every time step
        // dt<0 means update only once upon initialization
        if(this.dt==0 && (link.model() instanceof AbstractFluidModel))
            this.dt = ((AbstractFluidModel)link.model()).dt_sec;

    }

    private InterfaceLaneSelector create_lane_selector(AbstractLaneGroup lg,String type,jaxb.Parameters params) throws OTMException {
        switch(type) {
            case "toll":
                return new TollLaneSelector(lg,params);
            case "logit":
                return new LogitLaneSelector(lg,params);
            case "uniform":
                return new UniformLaneSelector();
            case "keep":
                return new KeepLaneSelector();
            default:
                throw new OTMException("Unknown lane change type");
        }
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

//    public final void add_maneuver(State state, Maneuver maneuver){
//        if(side2prob.containsKey(state.pathOrlink_id)){
//            Map<Maneuver,Double> e = side2prob.get(state.pathOrlink_id);
//            if(!e.containsKey(maneuver))
//                e.put(maneuver,0d);
//        } else {
//            Map<Maneuver,Double> e = new HashMap<>();
//            side2prob.put(state.pathOrlink_id,e);
//            e.put(maneuver,0d);
//        }
//    }
//
//    public final Map<Maneuver,Double> get_lanechange_probabilities(Long pathorlinkid){
//        return side2prob.get(pathorlinkid);
//    }

    public final void update_lane_change_probabilities() {
//        for(Long pathorlinkid : side2prob.keySet())
//            update_lane_change_probabilities_with_options(pathorlinkid,side2prob.get(pathorlinkid).keySet());
    }

}
