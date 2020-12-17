package lanechange;

import core.AbstractLaneGroup;
import models.Maneuver;

import java.util.Map;
import java.util.Set;

public class KeepLaneSelector extends AbstractLaneSelector {

    public KeepLaneSelector(AbstractLaneGroup lg,Long commid) {
        super(lg, 0f,commid);
    }

    @Override
    public void update_lane_change_probabilities_with_options(Long pathorlinkid, Set<Maneuver> lcoptions) {

        Map<Maneuver,Double> myside2prob = side2prob.get(pathorlinkid);

        if(lcoptions.size()==1){
            Maneuver m = lcoptions.iterator().next();
            myside2prob.clear();
            myside2prob.put(m,1d);
            return;
        }

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
            myside2prob.put(Maneuver.lcin,0d);

        if(has_middle)
            myside2prob.put(Maneuver.stay,1d);

        if(has_out)
            myside2prob.put(Maneuver.lcout,0d);

    }

}
