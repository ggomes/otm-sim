package lanechange;

import core.AbstractLaneGroup;
import core.Link;
import core.Scenario;
import core.State;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import dispatch.Pokable;
import error.OTMException;
import core.AbstractFluidModel;

import java.util.*;

public class LinkLaneSelector implements Pokable {

    protected Float dt;
    protected Link  link;
    protected List<LaneGroupData> lgdatas;

    public LinkLaneSelector(Link link, Float dt){
        this.link = link;
        this.dt = dt;
        lgdatas = new ArrayList<>();

        for(AbstractLaneGroup lg : link.get_lgs() )
            lgdatas.add( new LaneGroupData(lg) );
    }

    public void add_type(String type, jaxb.Parameters params, Collection<Long> commids) throws OTMException {
        for(LaneGroupData lgdata : lgdatas)
            lgdata.add_type(type,params,commids);
    }

    public void initialize(Scenario scenario, float start_time) throws OTMException {
        poke(scenario.dispatcher,start_time);
    }

    @Override
    public void poke(Dispatcher dispatcher, float timestamp) throws OTMException {
        
        for(LaneGroupData lgdata : lgdatas)
            for(Map.Entry<State, InterfaceLaneSelector> e2 : lgdata.lcs.entrySet())
                e2.getValue().update_lane_change_probabilities_with_options(lgdata.lg,e2.getKey());

        if(dt!=null)
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

//    public final Map<Maneuver,Double> get_lanechange_probabilities(Long pathorlinkid){
//        return side2prob.get(pathorlinkid);
//    }

    public Map<State, InterfaceLaneSelector> get_laneselector_for_lanegroup(long lgid){
        Optional<LaneGroupData> x = lgdatas.stream().filter(lgd->lgd.lg.getId()==lgid).findFirst();
        return x.isPresent() ? x.get().lcs : null;
    }

    protected class LaneGroupData {
        public AbstractLaneGroup lg;
        public Map<State, InterfaceLaneSelector> lcs;     // state -> algorithm
        public LaneGroupData(AbstractLaneGroup lg) {
            this.lg = lg;
            lcs = new HashMap<>();
        }
        public void add_type(String type, jaxb.Parameters params, Collection<Long> commids) throws OTMException {
            for (State state : lg.get_states())
                if (commids.contains(state.commodity_id))
                    lcs.put(state, create_lane_selector(lg, type, params));
        }

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

}
