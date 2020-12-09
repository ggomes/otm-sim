package lanechange;

import core.AbstractLaneGroup;
import core.geometry.Side;
import models.fluid.FluidLaneGroup;

import java.util.Map;
import java.util.Set;

public class LogitLaneSelector extends AbstractLaneSelector {

    private double keep = 0.7;                  // [-] positive utility of keeping your lane
    private double rho_vehperlane = 0.018504;   // [1/vehperlane] positive utility of changing lanes into a lane with lower density
    private double add_in;  // additional terms used for setting toll on hot lane
    private final double threshold = 1.05d;

    public LogitLaneSelector(AbstractLaneGroup lg, float dt,jaxb.Parameters params,Long commid) {
        super(lg,dt,commid);
        if(params!=null){
            for(jaxb.Parameter p : params.getParameter()){
                switch(p.getName()){
                    case "keep":
                        this.keep = Math.abs(Float.parseFloat(p.getValue()));
                        break;
                    case "rho_vpkmplane":
                        this.rho_vehperlane = Math.abs(Float.parseFloat(p.getValue()))/(lg.length/1000.0);
                        break;
                }
            }
        }
        this.add_in = 0d;
    }
    public LogitLaneSelector(AbstractLaneGroup lg, float dt,float keep,float rho_vpkmplane,Long commid) {
        super(lg,dt,commid);
        this.keep = keep;
        this.rho_vehperlane = rho_vpkmplane;
        this.add_in = 0d;
    }

    @Override
    public void update_lane_change_probabilities_with_options(Long pathorlinkid, Set<Side> lcoptions) {

        assert(lcoptions.size()>0);

        Map<Side,Double> myside2prob = side2prob.get(pathorlinkid);

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
        double ei=0d;
        double em=0d;
        double eo=0d;

        boolean has_in = lcoptions.contains(Side.in) && lg.neighbor_in!=null;
        if(has_in) {
            if(Double.isInfinite(add_in))
                ei = 0;
            else{
                FluidLaneGroup tlg = (FluidLaneGroup)lg.neighbor_in;
                ui = Math.min(0d, rho_vehperlane * (tlg.critical_density_veh-tlg.get_total_vehicles() ) / tlg.num_lanes );
//                ui = rho_vehperlane * (tlg.critical_density_veh-tlg.get_total_vehicles() ) / tlg.num_lanes;
                ui -= add_in;
                ei = Math.exp(ui);
            }
        }

        boolean has_middle = lcoptions.contains(Side.middle);
        if(has_middle) {
            FluidLaneGroup tlg = (FluidLaneGroup)lg;
            um = Math.min(0d,rho_vehperlane * (tlg.critical_density_veh-tlg.get_total_vehicles()) / tlg.num_lanes );
//            um = rho_vehperlane * (tlg.critical_density_veh-tlg.get_total_vehicles()) / tlg.num_lanes;
            um += keep;
            em = Math.exp( um );
        }

        boolean has_out = lcoptions.contains(Side.out) && lg.neighbor_out!=null;
        if(has_out) {
            FluidLaneGroup tlg = (FluidLaneGroup)lg.neighbor_out;
            uo = Math.min(0d,rho_vehperlane * (tlg.critical_density_veh-tlg.get_total_vehicles()) / tlg.num_lanes );
//            uo = rho_vehperlane * (tlg.critical_density_veh-tlg.get_total_vehicles()) / tlg.num_lanes;
            eo = Math.exp(uo);
        }

        // thresholding
        if(has_in && ui<threshold*um)
            ei = 0d;
        if(has_out && uo<threshold*um)
            eo = 0d;

        den = ei+em+eo;

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

    public double getKeep() {
        return keep;
    }

    public void setKeep(double keep) {
        this.keep = keep;
    }

    public double getRho_vehperlane() {
        return rho_vehperlane;
    }

    public void setRho_vehperlane(double rho_vehperlane) {
        this.rho_vehperlane = rho_vehperlane;
    }

    public double getAdd_in() {
        return add_in;
    }

    public void setAdd_in(double add_in) {
        this.add_in = add_in;
    }

}
