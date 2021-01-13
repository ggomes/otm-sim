package lanechange;

import core.Link;
import core.Scenario;
import utils.OTMUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanegroupLogitLaneSelector extends AbstractLaneSelector {

    Map<Long, CommData> commdatas; // commid->CommData

    public LanegroupLogitLaneSelector(Scenario scenario, Link link, Float dt, List<jaxb.Lanechange> lcs) {
        super(link,dt);

        commdatas = new HashMap<>();
        for(jaxb.Lanechange jlc : lcs){

            Collection<Long> commids = jlc.getComms() == null ?
                    scenario.commodities.keySet() :
                    OTMUtils.csv2longlist(jlc.getComms());

            double keep = 0.7;
            double rho_vehperlane = 0.018504;
            double add_in = 0d;
//            if(jlc.getParameters()!=null){
//                for(jaxb.Parameter p : jlc.getParameters().getParameter()){
//                    switch(p.getName()){
//                        case "keep":
//                            keep = Math.abs(Float.parseFloat(p.getValue()));
//                            break;
//                        case "rho_vpkmplane":
//                            rho_vehperlane = Math.abs(Float.parseFloat(p.getValue()))/(lg.get_length()/1000.0);
//                            break;
//                        case "add_in":
//                            add_in = Math.abs(Float.parseFloat(p.getValue()))/(lg.get_length()/1000.0);
//                            break;
//                    }
//                }
//            }

            for(long commid : commids)
                commdatas.put(commid,new CommData(keep,rho_vehperlane,add_in));

        }

    }

    @Override
    protected void update() {

//        CommData cd = commdatas.get(state.commodity_id);
//
//        Map<Maneuver,Double> mnv2prob = lg.get_maneuvprob_for_state(state);
//
//        if(mnv2prob.size()==1){
//            Maneuver mnv = mnv2prob.keySet().iterator().next();
//            mnv2prob.clear();
//            mnv2prob.put(mnv,1d);
//            return;
//        }
//
//        double den = 0d;
//        double ui=0d;
//        double um=0d;
//        double uo=0d;
//        double ei=0d;
//        double em=0d;
//        double eo=0d;
//
//        boolean has_in = mnv2prob.containsKey(Maneuver.lcin) && lg.get_neighbor_in()!=null;
//        if(has_in) {
//            if(Double.isInfinite(cd.add_in))
//                ei = 0;
//            else{
//                FluidLaneGroup tlg = (FluidLaneGroup)lg.get_neighbor_in();
//                ui = Math.min(0d, cd.rho_vehperlane * (tlg.critical_density_veh-tlg.get_total_vehicles() ) / tlg.get_num_lanes() );
//                ui -= cd.add_in;
//                ei = Math.exp(ui);
//            }
//        }
//
//        boolean has_middle = mnv2prob.containsKey(Maneuver.stay);
//        if(has_middle) {
//            FluidLaneGroup tlg = (FluidLaneGroup)lg;
//            um = Math.min(0d,cd.rho_vehperlane * (tlg.critical_density_veh-tlg.get_total_vehicles()) / tlg.get_num_lanes() );
//            um += cd.keep;
//            em = Math.exp( um );
//        }
//
//        boolean has_out = mnv2prob.containsKey(Maneuver.lcout) && lg.get_neighbor_out()!=null;
//        if(has_out) {
//            FluidLaneGroup tlg = (FluidLaneGroup)lg.get_neighbor_out();
//            uo = Math.min(0d,cd.rho_vehperlane * (tlg.critical_density_veh-tlg.get_total_vehicles()) / tlg.get_num_lanes() );
//            eo = Math.exp(uo);
//        }
//
//        // thresholding
//        if(has_in && ui<cd.threshold*um)
//            ei = 0d;
//        if(has_out && uo<cd.threshold*um)
//            eo = 0d;
//
//        den = ei+em+eo;
//
//        if(has_in)
//            mnv2prob.put(Maneuver.lcin,ei/den);
//
//        if(has_middle)
//            mnv2prob.put(Maneuver.stay,em/den);
//
//        if(has_out)
//            mnv2prob.put(Maneuver.lcout,eo/den);
//
    }

    public class CommData {
        public double keep;             // [-] positive utility of keeping your lane
        public double rho_vehperlane;   // [1/vehperlane] positive utility of changing lanes into a lane with lower density
        public double add_in;  // additional terms used for setting toll on hot lane
        public final double threshold = 1.05d;

        public CommData(double keep, double rho_vehperlane, double add_in) {
            this.keep = keep;
            this.rho_vehperlane = rho_vehperlane;
            this.add_in = add_in;
        }
    }
}
