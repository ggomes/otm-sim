package lanechange;

import core.AbstractLaneGroup;
import core.Link;
import core.Scenario;
import core.State;
import error.OTMException;
import jaxb.Lanechange;
import models.Maneuver;
import utils.OTMUtils;

import java.util.*;

public class LinkLinearLaneSelector extends AbstractLaneSelector {

    Map<Long, CommData> commdatas; // commid->CommData
    double [] lgsupplies;
    double threshold = 0f;

    public LinkLinearLaneSelector(Scenario scenario, Link link, Float dt, List<Lanechange> lcs) throws OTMException {
        super(link,dt);

        lgsupplies = new double[link.get_lgs().size()];

        commdatas = new HashMap<>();
        for(jaxb.Lanechange jlc : lcs) {

            Collection<Long> commids = jlc.getComms() == null ?
                    scenario.commodities.keySet() :
                    OTMUtils.csv2longlist(jlc.getComms());

            double epsilon = 0.8d;
            double alpha = 1d;
            double gamma = 30d;
            if (jlc.getParameters() != null) {
                for (jaxb.Parameter p : jlc.getParameters().getParameter()) {
                    switch (p.getName()) {
                        case "epsilon":
                            epsilon = Math.abs(Double.parseDouble(p.getValue()));
                            epsilon = Math.min(1d, Math.max(0d, epsilon));
                            break;
                        case "alpha":
                            alpha = Math.abs(Double.parseDouble(p.getValue()));
                            alpha = Math.max(0d, alpha);
                            break;
                        case "gamma":
                            gamma = Math.abs(Double.parseDouble(p.getValue()));
                            gamma = Math.max(0d, gamma);
                            break;
                        default:
                            throw new OTMException("Unknown parameter name in LinkLogitLaneSelector");
                    }
                }
            }

            for (long commid : commids)
                commdatas.put(commid, new CommData(epsilon, alpha, gamma, link.get_lgs()));

        }
    }

    public void set_toll(long commid,long lgid, double toll){
        if(toll<0d)
            return;
        if(commdatas.containsKey(commid))
            commdatas.get(commid).toll.put(lgid,toll);
    }

    public void set_toll_all_comm(long lgid, double toll){
        if(toll<0d)
            return;
        for(CommData c : commdatas.values())
            c.toll.put(lgid,toll);
    }

    public void set_toll_coeff_all_lgs_comm(double toll_coeff){
        if(toll_coeff<0d)
            return;
        for(CommData c : commdatas.values())
            c.alpha = toll_coeff;
    }


    @Override
    protected void update() {

        double Sa, Sb;  // lane group supply in veh/lane/meter
        double pab, pba, sigma_ab, sigma_ba;
         double taua, taub, alpha_ab;

        List<AbstractLaneGroup> lgs = link.get_lgs();

        // iterate though adjacent lanegroup pairs
        // compute lane chanege probabilities for each commodity
        for(int i=0;i<lgs.size()-1;i++){

            AbstractLaneGroup lga = lgs.get(i);
            AbstractLaneGroup lgb = lgs.get(i+1);

            Sa = lga.get_lat_supply() / lga.get_num_lanes() / lga.get_length();
            Sb = lgb.get_lat_supply() / lgb.get_num_lanes() / lgb.get_length();

            for( CommData c : commdatas.values() ){

                taua = c.toll.get(lga.getId());
                taub = c.toll.get(lgb.getId());

                pab = 0d;
                pba = 0d;

                alpha_ab = c.alpha * ( taua - taub );
                sigma_ab =  c.epsilon*Sb - Sa + alpha_ab - threshold;

                // flow from a to b
                if( sigma_ab > 0 )
                    pab = Math.min( 1d , Math.max(0d, c.gamma * sigma_ab ) );

                // flow from b to a
                else{
                    sigma_ba = c.epsilon*Sa - Sb - alpha_ab - threshold;
                    if( sigma_ba > 0 )
                        pba = Math.min(1d, Math.max(0d, c.gamma * sigma_ba ) );
                }

                c.lg_mnv2prob.get(i).put(Maneuver.lcout,pab);
                c.lg_mnv2prob.get(i+1).put(Maneuver.lcin,pba);
            }

        }

        // iterate though lanegroups. For each, iterate through states.
        // extract probabilities corresponding to available
        for(int i=0;i<lgs.size();i++){

            AbstractLaneGroup lg = lgs.get(i);

            for(State state : lg.get_link().states ){

                Map<Maneuver,Double> lg_mnv2prob = lg.get_maneuvprob_for_state(state);

                if( lg_mnv2prob.size()==1 )
                    lg_mnv2prob.put(lg_mnv2prob.keySet().iterator().next(),1d);
                else {
                    CommData c = commdatas.get(state.commodity_id);
                    Map<Maneuver,Double> local_mnv2prob =c.lg_mnv2prob.get(i);

                    boolean has_in = lg_mnv2prob.containsKey(Maneuver.lcin);
                    boolean has_stay = lg_mnv2prob.containsKey(Maneuver.stay);
                    boolean has_out = lg_mnv2prob.containsKey(Maneuver.lcout);

                    double pin = has_in ? local_mnv2prob.get(Maneuver.lcin) : 0d;
                    double pout = has_out ? local_mnv2prob.get(Maneuver.lcout) : 0d;

                    double s = pin + pout;
                    double pstay;
                    if(s<=1 && has_stay)
                        pstay = 1-s;
                    else{
                        pstay = 0d;
                        pin /= s;
                        pout /= s;
                    }

                    if(has_in)
                        lg_mnv2prob.put(Maneuver.lcin,pin);
                    if(has_stay)
                        lg_mnv2prob.put(Maneuver.stay,pstay);
                    if(has_out)
                        lg_mnv2prob.put(Maneuver.lcout,pout);
                }

            }

        }

    }

    public class CommData {
        public double epsilon;  // in [0,1] supply reduction factor for adjacent lane groups
        public double alpha;    // non-negative toll price multiplier
        public double gamma;
        public Map<Long, Double> toll;  // lg->toll in cents
        public List< Map<Maneuver,Double> > lg_mnv2prob;

        public CommData(double epsilon, double alpha, double gamma, List<AbstractLaneGroup> lgs) {
            this.epsilon = epsilon;
            this.alpha = alpha;
            this.gamma = gamma;
            this.toll = new HashMap<>();
            lg_mnv2prob = new ArrayList<>();
            int numlgs = lgs.size();
            for(int i=0;i<lgs.size();i++){
                Map<Maneuver,Double> x = new HashMap<>();
                if(i>0)
                    x.put(Maneuver.lcin,0d);
                if(i<numlgs-1)
                    x.put(Maneuver.lcout,0d);
                lg_mnv2prob.add(x);
                toll.put(lgs.get(i).getId(), 0d);
            }
        }
    }

}
