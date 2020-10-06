package lanechange;

import common.AbstractLaneGroup;
import geometry.Side;
import keys.State;

import java.util.Map;
import java.util.Set;

public class LogitLaneSelector extends AbstractLaneSelector {

    private double a_keep = 1f;                // [-] positive utility of keeping your lane
    private double a_rho_vehperlane = 1f;    // [1/vehperlane] positive utility of changing lanes into a lane with lower density
//    private double a_toll;                    // [1/cents] positive utility of not paying the toll

    public LogitLaneSelector(AbstractLaneGroup lg, float dt,jaxb.Parameters params) {
        super(lg,dt);
        if(params!=null){
            for(jaxb.Parameter p : params.getParameter()){
                switch(p.getName()){
                    case "keep":
                        this.a_keep = Float.parseFloat(p.getValue());
                        break;
                    case "deltarho_vpkmplane":
                        this.a_rho_vehperlane = Float.parseFloat(p.getValue())/lg.length;
                        break;
                }
            }
        }
    }

    @Override
    public void update_lane_change_probabilities_with_options(State state, Set<Side> lcoptions) {

        assert(lcoptions.size()>0);

        Map<Side,Double> myside2prob = side2prob.get(state);

        if(lcoptions.size()==1){
            Side lcoption = lcoptions.iterator().next();
            myside2prob.clear();
            myside2prob.put(lcoption,1d);
            return;
        }

        double den = 0d;

        boolean has_in = lcoptions.contains(Side.in) && lg.neighbor_in!=null;
        double ei=0d;
        if(has_in) {
            ei = Math.exp( -a_rho_vehperlane *lg.neighbor_in.get_total_vehicles()/lg.neighbor_in.num_lanes);
            den += ei;
        }

        boolean has_middle = lcoptions.contains(Side.middle);
        double em=0d;
        if(has_middle) {
            em = Math.exp(a_keep - a_rho_vehperlane * lg.get_total_vehicles()/lg.num_lanes);
            den += em;
        }

        boolean has_out = lcoptions.contains(Side.out) && lg.neighbor_out!=null;
        double eo=0d;
        if(has_out) {
            eo = Math.exp( -a_rho_vehperlane *lg.neighbor_out.get_total_vehicles()/lg.neighbor_out.num_lanes );
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
