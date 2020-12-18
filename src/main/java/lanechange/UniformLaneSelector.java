package lanechange;

import core.AbstractLaneGroup;
import core.Link;
import core.State;
import models.Maneuver;

import java.util.Map;

public class UniformLaneSelector implements InterfaceLaneSelector {


    @Override
    public void update_lane_change_probabilities_with_options(AbstractLaneGroup lg, State state) {

        Map<Maneuver,Double> mnv2prob = lg.state2lanechangeprob.get(state);

        boolean has_in = mnv2prob.containsKey(Maneuver.lcin) && lg.neighbor_in!=null;
        boolean has_stay = mnv2prob.containsKey(Maneuver.stay);
        boolean has_out = mnv2prob.containsKey(Maneuver.lcout) && lg.neighbor_out!=null;

        double lanes_in = has_in ? lg.neighbor_in.num_lanes : 0d;
        double lanes_stay = has_stay ? lg.num_lanes : 0d;
        double lanes_out = has_out ? lg.neighbor_out.num_lanes : 0d;
        double lanes_total = lanes_in + lanes_stay + lanes_out;

        if(has_in)
            mnv2prob.put(Maneuver.lcin,lanes_in/lanes_total);

        if(has_stay)
            mnv2prob.put(Maneuver.stay,lanes_stay/lanes_total);

        if(has_out)
            mnv2prob.put(Maneuver.lcout,lanes_out/lanes_total);
    }

}
