package models.fluid;

import commodity.Commodity;
import commodity.Path;
import core.AbstractDemandGenerator;
import core.Link;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import core.State;
import core.AbstractLaneGroup;
import profiles.Profile1D;
import utils.OTMUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FluidDemandGenerator extends AbstractDemandGenerator {

    // for pathfull
    Set<AbstractLaneGroup> pathfull_lgs;

    public FluidDemandGenerator(Link link, Profile1D profile, Commodity commodity, Path path) {
        super(link, profile, commodity, path);

        if(commodity.pathfull) {
            Link next_link = path.get_link_following(link);
            pathfull_lgs = link.outlink2lanegroups.get(next_link.getId());
        }
    }

    public void validate(OTMErrorLog errorLog){

        if(!commodity.pathfull) {

            if(link.lgs.size()!=1)
                errorLog.addError(String.format("Source on link %d with more than one lane group",link.getId()));

            // TODO DEAL WITH THIS RESTRICTION
//            if(!link.end_node.is_many2one)
//                errorLog.addError(String.format("Source on link %d, whose end node %d has multiple outputs",link.getId(),link.end_node.getId()));

        }
    }

    @Override
    public void set_demand_vps(Dispatcher dispatcher, float time, double new_demand_vps) throws OTMException {
        super.set_demand_vps(dispatcher, time, new_demand_vps);

        double flow_veh_per_timestep = source_demand_vps*((AbstractFluidModel)link.model).dt_sec;
        Long comm_id = commodity.getId();

        if(commodity.pathfull){
            final State state = new State(comm_id,path.getId(),true);
            double demand_for_each_lg = flow_veh_per_timestep / pathfull_lgs.size();
            for(AbstractLaneGroup lg : pathfull_lgs)
                ((FluidLaneGroup)lg).source_flow.put(state,demand_for_each_lg);
        }

        // source of pathless commodity
        else {

            // Case no downstream split.
            if(link.outlink2lanegroups.size()<2){

                Long nextlink_id = link.outlink2lanegroups.keySet().iterator().next();
                State state = new State(comm_id,nextlink_id,false);

                List<Double> capacities = link.lgs.stream()
                        .map(lg->((FluidLaneGroup)lg).capacity_veh_per_dt)
                        .collect(Collectors.toList());
                double sum = capacities.stream().reduce(0d,Double::sum);

                for(int i = 0; i<link.lgs.size(); i++){
                    FluidLaneGroup lg = (FluidLaneGroup)link.lgs.get(i);
                    lg.source_flow.put(state,flow_veh_per_timestep*capacities.get(i)/sum);
                }

            }

            // Otherwise...
            else {

                Map<Long,Double> outlink2split = link.get_splits_for_commodity(comm_id);

                // for each out link in the spit ratio matrix, assign a portion of
                // the source flow to the appropriate lane groups.
                for(Map.Entry<Long,Double> e : outlink2split.entrySet() ){
                    Long nextlink_id = e.getKey();
                    Double split = e.getValue();
                    State state = new State(comm_id,nextlink_id,false);

                    if(!OTMUtils.greater_than(split,0d))
                        continue;

                    // get candidate lanegroups
                    Set<AbstractLaneGroup> candidate_lanegroups = link.outlink2lanegroups.get(nextlink_id);

                    // assign flows to candidate lanegroups
                    double all_lanes = candidate_lanegroups.stream().mapToDouble(x->x.num_lanes).sum();
                    double factor = flow_veh_per_timestep * split / all_lanes;

                    for(AbstractLaneGroup alg : candidate_lanegroups)
                        ((FluidLaneGroup) alg).source_flow.put(state,factor * alg.num_lanes);

                }
            }
        }

    }

}
