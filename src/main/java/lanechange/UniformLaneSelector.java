package lanechange;

import core.AbstractLaneGroup;
import models.Maneuver;

import java.util.Map;
import java.util.Set;

public class UniformLaneSelector extends AbstractLaneSelector {

    public UniformLaneSelector(AbstractLaneGroup lg,Long commid) {
        super(lg, 0f,commid);
    }

    @Override
    public void update_lane_change_probabilities_with_options(Long pathorlinkid, Set<Maneuver> lcoptions) {

        Map<Maneuver,Double> myside2prob = side2prob.get(pathorlinkid);

        double s = 1d/lcoptions.size();
        boolean has_in = lcoptions.contains(Maneuver.lcin) && lg.neighbor_in!=null;
        boolean has_middle = lcoptions.contains(Maneuver.stay);
        boolean has_out = lcoptions.contains(Maneuver.lcout) && lg.neighbor_out!=null;

        // clean side2prob
        if(myside2prob.containsKey(Maneuver.lcin) && !has_in)
            myside2prob.remove(Maneuver.lcin);
        if(myside2prob.containsKey(Maneuver.stay) && !has_middle)
            myside2prob.remove(Maneuver.stay);
        if(myside2prob.containsKey(Maneuver.lcout) && !has_out)
            myside2prob.remove(Maneuver.lcout);

        if(has_in)
            myside2prob.put(Maneuver.lcin,s);

        if(has_middle)
            myside2prob.put(Maneuver.stay,s);

        if(has_out)
            myside2prob.put(Maneuver.lcout,s);
    }

}
