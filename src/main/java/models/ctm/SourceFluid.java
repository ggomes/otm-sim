package models.ctm;

import commodity.Commodity;
import commodity.Path;
import common.*;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import keys.KeyCommPathOrLink;
import models.AbstractLaneGroup;
import profiles.DemandProfile;
import utils.OTMUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SourceFluid extends common.AbstractSource {

    public Map<Long,Map<KeyCommPathOrLink,Double>> source_flows;   // lgid->(key->value)

    // for pathfull
    Set<AbstractLaneGroup> candidate_lanegroups;

    public SourceFluid(Link link, DemandProfile profile, Commodity commodity, Path path) {
        super(link, profile, commodity, path);

        if(commodity.pathfull) {
            Link next_link = path.get_link_following(link);
            candidate_lanegroups = link.outlink2lanegroups.get(next_link.getId());
        }
    }

    public void validate(OTMErrorLog errorLog){

        if(!commodity.pathfull) {

            if(link.lanegroups_flwdn.size()!=1)
                errorLog.addError(String.format("Source on link %d with more than one lane group",link.getId()));

            // TODO DEAL WITH THIS RESTRICTION
//            if(!link.end_node.is_many2one)
//                errorLog.addError(String.format("Source on link %d, whose end node %d has multiple outputs",link.getId(),link.end_node.getId()));

        }
    }

    @Override
    public void set_demand_vps(Dispatcher dispatcher, float time, double value) throws OTMException {
        super.set_demand_vps(dispatcher, time, value);
        source_flows = split_demand(
                source_demand_vps*((models.ctm.Model_CTM)link.model).dt
        );
    }

    private Map<Long,Map<KeyCommPathOrLink,Double>> split_demand(double flow_veh_per_timestep){

        Long comm_id = commodity.getId();

        // for each lanegroup, a map from state to value.
        Map<Long,Map<KeyCommPathOrLink,Double>> source_flows = new HashMap<>();

        if(commodity.pathfull){
            KeyCommPathOrLink key = new KeyCommPathOrLink(comm_id,path.getId(),true);
            double demand_for_each_lg = flow_veh_per_timestep / candidate_lanegroups.size();
            for(AbstractLaneGroup lg : candidate_lanegroups) {
                Map<KeyCommPathOrLink,Double> x = new HashMap<>();
                x.put(key,demand_for_each_lg);
                source_flows.put(lg.id, x);
            }
        }

        // source of pathless commodity
        else {

            // Case no downstream split.
            if(link.outlink2lanegroups.size()<2){

                Long nextlink_id = link.outlink2lanegroups.keySet().iterator().next();
                KeyCommPathOrLink key = new KeyCommPathOrLink(comm_id,nextlink_id,false);

                AbstractLaneGroup lg = link.lanegroups_flwdn.values().iterator().next();
                Map<KeyCommPathOrLink,Double> x = new HashMap<>();
                x.put(key,flow_veh_per_timestep);
                source_flows.put(lg.id,x);
                return source_flows;
            }


            // Otherwise...
            Map<Long,Double> outlink2split = link.get_splits_for_commodity(comm_id);

            // for each out link in the spit ratio matrix, assign a portion of
            // the source flow to the appropriate lane groups.
            for(Map.Entry<Long,Double> e : outlink2split.entrySet() ){
                Long nextlink_id = e.getKey();
                Double split = e.getValue();
                KeyCommPathOrLink key = new KeyCommPathOrLink(comm_id,nextlink_id,false);

                if(!OTMUtils.greater_than(split,0d))
                    continue;

                // get candidate lanegroups
                Set<AbstractLaneGroup> candidate_lanegroups = link.outlink2lanegroups.get(nextlink_id);

                // assign flows to candidate lanegroups
                double all_lanes = candidate_lanegroups.stream().mapToDouble(x->x.num_lanes).sum();
                double factor = flow_veh_per_timestep * split / all_lanes;

                for(AbstractLaneGroup lg : candidate_lanegroups){
                    Map<KeyCommPathOrLink,Double> x;
                    double demand_for_lg = factor * lg.num_lanes;
                    if(source_flows.containsKey(lg.id)) {
                        x = source_flows.get(lg.id);
                        double val = x.containsKey(key) ? x.get(key) : 0d;
                        x.put(key,val + demand_for_lg);
                    }
                    else {
                        x = new HashMap<>();
                        x.put(key,demand_for_lg);
                    }
                    source_flows.put(lg.id,x);
                }

            }
        }
        return source_flows;
    }
}
