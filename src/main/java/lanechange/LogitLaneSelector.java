package lanechange;

import common.AbstractLaneGroup;
import geometry.Side;
import keys.State;
import models.fluid.FluidLaneGroup;

import java.util.Map;
import java.util.Set;

public class LogitLaneSelector extends AbstractLaneSelector {

    private double a_keep = 0.7;                  // [-] positive utility of keeping your lane
    private double a_rho_vehperlane = 0.018504;   // [1/vehperlane] positive utility of changing lanes into a lane with lower density
//    private double a_toll;                    // [1/cents] positive utility of not paying the toll


    public LogitLaneSelector(AbstractLaneGroup lg, float dt,jaxb.Parameters params) {
        super(lg,dt);
        if(params!=null){
            for(jaxb.Parameter p : params.getParameter()){
                switch(p.getName()){
                    case "keep":
                        this.a_keep = Math.abs(Float.parseFloat(p.getValue()));
                        break;
                    case "rho_vpkmplane":
                        this.a_rho_vehperlane = Math.abs(Float.parseFloat(p.getValue()))/(lg.length/1000.0);
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
        double ui=0d;
        double um=0d;
        double uo=0d;

        boolean has_in = lcoptions.contains(Side.in) && lg.neighbor_in!=null;
        double ei=0d;
        if(has_in) {
            FluidLaneGroup tlg = (FluidLaneGroup)lg.neighbor_in;
//            ei = Math.exp( -a_rho_vehperlane *lg.neighbor_in.get_total_vehicles()/lg.neighbor_in.num_lanes);
//            ei = Math.exp( a_rho_vehperlane * targetlg.get_upstream_cell().supply /targetlg.num_lanes);
            ui = Math.min(0d,tlg.critical_density_vehpercell-tlg.get_total_vehicles());
            ei = Math.exp( a_rho_vehperlane * ui );
            den += ei;
        }

        boolean has_middle = lcoptions.contains(Side.middle);
        double em=0d;
        if(has_middle) {
            FluidLaneGroup tlg = (FluidLaneGroup)lg;
//            em = Math.exp(a_keep + a_rho_vehperlane * targetlg.get_dnstream_cell().supply /targetlg.num_lanes);
            um = Math.min(0d,tlg.critical_density_vehpercell-tlg.get_total_vehicles());
            em = Math.exp( a_keep + a_rho_vehperlane * um );
            den += em;
        }

        boolean has_out = lcoptions.contains(Side.out) && lg.neighbor_out!=null;
        double eo=0d;
        if(has_out) {
            FluidLaneGroup tlg = (FluidLaneGroup)lg.neighbor_out;
//            eo = Math.exp( a_rho_vehperlane * targetlg.get_upstream_cell().supply /targetlg.num_lanes);

            uo = Math.min(0d,tlg.critical_density_vehpercell -tlg.get_total_vehicles());
            eo = Math.exp( a_rho_vehperlane * uo ) ;

            den += eo;
        }

        // clean side2prob
        if(myside2prob.containsKey(Side.in) && !has_in)
            myside2prob.remove(Side.in);
        if(myside2prob.containsKey(Side.middle) && !has_middle)
            myside2prob.remove(Side.middle);
        if(myside2prob.containsKey(Side.out) && !has_out)
            myside2prob.remove(Side.out);

        if(lg.link.getId()==1l) {
            float timestamp = lg.link.network.scenario.dispatcher.current_time;
            if (lg.neighbor_in != null) {
                FluidLaneGroup nlg = (FluidLaneGroup) lg.neighbor_in;
                System.out.println(String.format("%.1f\t( %.2f , %.2f , %.2f )\t( %.2f , %.2f )\t( %.2f , %.2f )",
                        timestamp, ei / den, em / den, eo / den,
                        nlg.critical_density_vehpercell,ui, //nlg.get_total_vehicles(),
                        ((FluidLaneGroup)lg).critical_density_vehpercell,um // lg.get_total_vehicles()
                ));
            }
        }


        if(has_in)
            myside2prob.put(Side.in,ei/den);

        if(has_middle)
            myside2prob.put(Side.middle,em/den);

        if(has_out)
            myside2prob.put(Side.out,eo/den);
    }

}
