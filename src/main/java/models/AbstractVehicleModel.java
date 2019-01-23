package models;

import commodity.Commodity;
import commodity.Path;
import common.AbstractSource;
import common.AbstractVehicle;
import common.Link;
import keys.KeyCommPathOrLink;
import profiles.DemandProfile;

import java.util.*;

public abstract class AbstractVehicleModel extends AbstractModel {

    public AbstractVehicleModel(String name, boolean is_default) {
        super(name, is_default);
    }

    //////////////////////////////////////////////////
    // factory
    //////////////////////////////////////////////////
    abstract public AbstractVehicle create_vehicle(Commodity comm);

    @Override
    public AbstractSource create_source(Link origin, DemandProfile demand_profile, Commodity commodity, Path path) {
        return new VehicleSource(origin,demand_profile,commodity,path);
    }

    //////////////////////////////////////////////////
    // run
    //////////////////////////////////////////////////

    @Override
    public Map<AbstractLaneGroup,Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups) {

        // put the whole packet i the lanegroup with the most space.
        Optional<? extends AbstractLaneGroup> best_lanegroup = candidate_lanegroups.stream()
                .max(Comparator.comparing(AbstractLaneGroup::get_space_per_lane));

        if(best_lanegroup.isPresent()) {
            Map<AbstractLaneGroup,Double> A = new HashMap<>();
            A.put(best_lanegroup.get(),1d);
            return A;
        } else
            return null;
    }
}
