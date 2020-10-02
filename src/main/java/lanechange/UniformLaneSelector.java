package lanechange;

import common.AbstractLaneGroup;
import geometry.Side;

import java.util.Set;

public class UniformLaneSelector extends AbstractLaneSelector {

    public UniformLaneSelector(AbstractLaneGroup lg) {
        super(lg, 0f);
    }

    @Override
    public void update_lane_change_probabilities_with_options(Set<Side> lcoptions) {

        double s = 1d/lcoptions.size();
        boolean has_in = lcoptions.contains(Side.in) && lg.neighbor_in!=null;
        boolean has_middle = lcoptions.contains(Side.middle);
        boolean has_out = lcoptions.contains(Side.out) && lg.neighbor_out!=null;

        // clean side2prob
        if(side2prob.containsKey(Side.in) && !has_in)
            side2prob.remove(Side.in);
        if(side2prob.containsKey(Side.middle) && !has_middle)
            side2prob.remove(Side.middle);
        if(side2prob.containsKey(Side.out) && !has_out)
            side2prob.remove(Side.out);

        if(has_in)
            side2prob.put(Side.in,s);

        if(has_middle)
            side2prob.put(Side.middle,s);

        if(has_out)
            side2prob.put(Side.out,s);
    }
}
