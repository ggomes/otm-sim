package lanechange;

import core.AbstractLaneGroup;
import core.State;
import models.Maneuver;

import java.util.Map;

public class KeepLaneSelector implements InterfaceLaneSelector {

    @Override
    public void update_lane_change_probabilities_with_options(AbstractLaneGroup lg, State state) {

        Map<Maneuver,Double> mnv2prob = lg.state2lanechangeprob.get(state);

        if(mnv2prob.size()==1){
            Maneuver m = mnv2prob.keySet().iterator().next();
            mnv2prob.clear();
            mnv2prob.put(m,1d);
            return;
        }

        boolean has_in = mnv2prob.containsKey(Maneuver.lcin) && lg.neighbor_in!=null;
        boolean has_stay = mnv2prob.containsKey(Maneuver.stay);
        boolean has_out = mnv2prob.containsKey(Maneuver.lcout) && lg.neighbor_out!=null;

        if(has_in)
            mnv2prob.put(Maneuver.lcin,0d);

        if(has_stay)
            mnv2prob.put(Maneuver.stay,1d);

        if(has_out)
            mnv2prob.put(Maneuver.lcout,0d);

    }

}
