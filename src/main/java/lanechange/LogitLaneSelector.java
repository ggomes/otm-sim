package lanechange;

import core.AbstractLaneGroup;
import core.State;
import models.Maneuver;
import models.fluid.FluidLaneGroup;

import java.util.Map;

public class LogitLaneSelector implements InterfaceLaneSelector {

    public final double keep;             // [-] positive utility of keeping your lane
    public final double rho_vehperlane;   // [1/vehperlane] positive utility of changing lanes into a lane with lower density
    public double add_in;  // additional terms used for setting toll on hot lane
    public final double threshold = 1.05d;

    public LogitLaneSelector(AbstractLaneGroup lg,jaxb.Parameters params) {

        double temp_keep = 0.7;
        double temp_rho_vehperlane = 0.018504;
        double temp_add_in = 0d;
        if(params!=null){
            for(jaxb.Parameter p : params.getParameter()){
                switch(p.getName()){
                    case "keep":
                        temp_keep = Math.abs(Float.parseFloat(p.getValue()));
                        break;
                    case "rho_vpkmplane":
                        temp_rho_vehperlane = Math.abs(Float.parseFloat(p.getValue()))/(lg.get_length()/1000.0);
                        break;
                    case "add_in":
                        temp_add_in = Math.abs(Float.parseFloat(p.getValue()))/(lg.get_length()/1000.0);
                        break;
                }
            }
        }
        this.keep = temp_keep;
        this.rho_vehperlane = temp_rho_vehperlane;
        this.add_in = temp_add_in;
    }

    public LogitLaneSelector(AbstractLaneGroup lg, float dt,float keep,float rho_vpkmplane,Long commid) {
        this.keep = keep;
        this.rho_vehperlane = rho_vpkmplane;
        this.add_in = 0d;
    }

    @Override
    public void update_lane_change_probabilities_with_options(AbstractLaneGroup lg, State state) {

        Map<Maneuver,Double> mnv2prob = lg.get_maneuvprob_for_state(state);

        if(mnv2prob.size()==1){
            Maneuver mnv = mnv2prob.keySet().iterator().next();
            mnv2prob.clear();
            mnv2prob.put(mnv,1d);
            return;
        }

        double den = 0d;
        double ui=0d;
        double um=0d;
        double uo=0d;
        double ei=0d;
        double em=0d;
        double eo=0d;

        boolean has_in = mnv2prob.containsKey(Maneuver.lcin) && lg.get_neighbor_in()!=null && mnv2prob.get(Maneuver.lcin)>0;
        if(has_in) {
            if(Double.isInfinite(add_in))
                ei = 0;
            else{
                FluidLaneGroup tlg = (FluidLaneGroup)lg.get_neighbor_in();
                ui = Math.min(0d, rho_vehperlane * (tlg.critical_density_veh-tlg.get_total_vehicles() ) / tlg.get_num_lanes() );
//                ui = rho_vehperlane * (tlg.critical_density_veh-tlg.get_total_vehicles() ) / tlg.num_lanes;
                ui -= add_in;
                ei = Math.exp(ui);
            }
        }

        boolean has_middle = mnv2prob.containsKey(Maneuver.stay);
        if(has_middle) {
            FluidLaneGroup tlg = (FluidLaneGroup)lg;
            um = Math.min(0d,rho_vehperlane * (tlg.critical_density_veh-tlg.get_total_vehicles()) / tlg.get_num_lanes() );
//            um = rho_vehperlane * (tlg.critical_density_veh-tlg.get_total_vehicles()) / tlg.num_lanes;
            um += keep;
            em = Math.exp( um );
        }

        boolean has_out = mnv2prob.containsKey(Maneuver.lcout) && lg.get_neighbor_out()!=null && mnv2prob.get(Maneuver.lcout)>0;
        if(has_out) {
            FluidLaneGroup tlg = (FluidLaneGroup)lg.get_neighbor_out();
            uo = Math.min(0d,rho_vehperlane * (tlg.critical_density_veh-tlg.get_total_vehicles()) / tlg.get_num_lanes() );
//            uo = rho_vehperlane * (tlg.critical_density_veh-tlg.get_total_vehicles()) / tlg.num_lanes;
            eo = Math.exp(uo);
        }

        // thresholding
        if(has_in && ui<threshold*um)
            ei = 0d;
        if(has_out && uo<threshold*um)
            eo = 0d;

        den = ei+em+eo;

        if(lg.get_link().getId()==6l && lg.get_start_lane_dn()==1)
            System.out.println(String.format("%.2f\t%.2f\t%.2f",ei,em,eo));

        // clean side2prob
        if(mnv2prob.containsKey(Maneuver.lcin) && !has_in)
            mnv2prob.remove(Maneuver.lcin);
        if(mnv2prob.containsKey(Maneuver.stay) && !has_middle)
            mnv2prob.remove(Maneuver.stay);
        if(mnv2prob.containsKey(Maneuver.lcout) && !has_out)
            mnv2prob.remove(Maneuver.lcout);

        if(has_in)
            mnv2prob.put(Maneuver.lcin,ei/den);

        if(has_middle)
            mnv2prob.put(Maneuver.stay,em/den);

        if(has_out)
            mnv2prob.put(Maneuver.lcout,eo/den);

    }

}
