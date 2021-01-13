package lanechange;

import core.Link;

public class UniformLaneSelector extends AbstractLaneSelector {

    public UniformLaneSelector(Link link) {
        super(link,null);
    }

    @Override
    protected void update() {
//        Map<Maneuver,Double> mnv2prob = lg.get_maneuvprob_for_state(state);
//
//        boolean has_in = mnv2prob.containsKey(Maneuver.lcin) && lg.get_neighbor_in()!=null;
//        boolean has_stay = mnv2prob.containsKey(Maneuver.stay);
//        boolean has_out = mnv2prob.containsKey(Maneuver.lcout) && lg.get_neighbor_out()!=null;
//
//        double lanes_in = has_in ? lg.get_neighbor_in().get_num_lanes() : 0d;
//        double lanes_stay = has_stay ? lg.get_num_lanes() : 0d;
//        double lanes_out = has_out ? lg.get_neighbor_out().get_num_lanes() : 0d;
//        double lanes_total = lanes_in + lanes_stay + lanes_out;
//
//        if(has_in)
//            mnv2prob.put(Maneuver.lcin,lanes_in/lanes_total);
//
//        if(has_stay)
//            mnv2prob.put(Maneuver.stay,lanes_stay/lanes_total);
//
//        if(has_out)
//            mnv2prob.put(Maneuver.lcout,lanes_out/lanes_total);
    }

}
