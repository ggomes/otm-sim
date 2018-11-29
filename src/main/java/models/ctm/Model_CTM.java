package models.ctm;

import common.AbstractLaneGroup;
import common.Link;
import error.OTMErrorLog;
import models.AbstractDiscreteTimeModel;

import java.util.*;

public class Model_CTM extends AbstractDiscreteTimeModel {

    public Float max_cell_length;

    public Model_CTM(Set<Link> links, String name, Float dt, Float max_cell_length) {
        super(links, name,dt);
        this.max_cell_length = max_cell_length;
    }


    @Override
    public void set_road_param(Link link,jaxb.Roadparam r, float sim_dt_sec) {

        if(Float.isNaN(sim_dt_sec))
            return;

        // adjustment for MN model
        if(link.model_type==Link.ModelType.mn)
            r.setJamDensity(Float.POSITIVE_INFINITY);

        // normalize
        float dt_hr = sim_dt_sec/3600f;
        float capacity_vehperlane = r.getCapacity()*dt_hr;

        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values()) {

            float jam_density_vehperlane = r.getJamDensity() * lg.length / 1000f;
            float ffspeed_veh = 1000f * r.getSpeed()*dt_hr / lg.length;

            lg.set_road_params(r);
            ((LaneGroup) lg).cells.forEach(c -> c.set_road_params(capacity_vehperlane, jam_density_vehperlane, ffspeed_veh));
        }
    }

    @Override
    public void validate(Link link,OTMErrorLog errorLog) {
    }

    @Override
    public void reset(Link link) {
        for(AbstractLaneGroup alg : link.lanegroups_flwdn.values()){
            LaneGroup lg = (LaneGroup) alg;
            lg.cells.forEach(x->x.reset());
        }
    }

    @Override
    public Map<AbstractLaneGroup,Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups) {
        Map<AbstractLaneGroup,Double> A = new HashMap<>();
        double total_supply = candidate_lanegroups.stream().mapToDouble(x->x.get_supply()).sum();
        for(AbstractLaneGroup laneGroup : candidate_lanegroups)
            A.put(laneGroup , laneGroup.get_supply() / total_supply);
        return A;
    }


}
