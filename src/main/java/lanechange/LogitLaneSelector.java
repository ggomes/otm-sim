package lanechange;

import common.AbstractLaneGroup;
import geometry.Side;
import keys.State;

import java.util.Map;
import java.util.Set;

public class LogitLaneSelector extends AbstractLaneSelector {

    private final double a0;
    private final double a1;
    private final double a2;

    public LogitLaneSelector(AbstractLaneGroup lg, float dt, double a0, double a1, double a2) {
        super(lg,dt);
        this.a0 = a0;
        this.a1 = a1;
        this.a2 = a2;
    }

    @Override
    public void update_lane_change_probabilities_with_options(State state, Set<Side> lcoptions) {

        assert(lcoptions.size()>0);

        Map<Side,Double> myside2prob = side2prob.get(state);

        if(lcoptions.size()==1){
            myside2prob.clear();
            myside2prob.put(lcoptions.iterator().next(),1d);
            return;
        }

        double den = 0d;

        boolean has_in = lcoptions.contains(Side.in) && lg.neighbor_in!=null;
        double ei=0d;
        if(has_in) {
            ei = Math.exp( -a1*lg.neighbor_in.get_total_vehicles());
            den += ei;
        }

        boolean has_middle = lcoptions.contains(Side.middle);
        double em=0d;
        if(has_middle) {
            em = Math.exp(a0 - a1 * lg.get_total_vehicles()/lg.num_lanes);
            den += em;
        }

        boolean has_out = lcoptions.contains(Side.out) && lg.neighbor_out!=null;
        double eo=0d;
        if(has_out) {
            eo = Math.exp( -a1*lg.neighbor_out.get_total_vehicles()/lg.neighbor_out.num_lanes );
            den += eo;
        }

        // clean side2prob
        if(myside2prob.containsKey(Side.in) && !has_in)
            myside2prob.remove(Side.in);
        if(myside2prob.containsKey(Side.middle) && !has_middle)
            myside2prob.remove(Side.middle);
        if(myside2prob.containsKey(Side.out) && !has_out)
            myside2prob.remove(Side.out);

        if(has_in)
            myside2prob.put(Side.in,ei/den);

        if(has_middle)
            myside2prob.put(Side.middle,em/den);

        if(has_out)
            myside2prob.put(Side.out,eo/den);

    }
}