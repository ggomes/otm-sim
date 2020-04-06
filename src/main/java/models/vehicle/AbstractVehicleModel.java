package models.vehicle;

import commodity.Commodity;
import commodity.Path;
import common.AbstractSource;
import common.Link;
import error.OTMException;
import models.AbstractLaneGroup;
import models.AbstractModel;
import profiles.DemandProfile;
import utils.StochasticProcess;

import java.util.*;

public abstract class AbstractVehicleModel extends AbstractModel implements InterfaceVehicleModel {

    public AbstractVehicleModel(String name, boolean is_default, StochasticProcess process) {
        super(AbstractModel.Type.Vehicle,name, is_default,process);
    }

    @Override
    public final void build() throws OTMException { }

    @Override
    public final void set_links(Set<Link> links) {
        super.set_links(links);
    }
//////////////////////////////////////////////////////////////
    // sample implementation
    //////////////////////////////////////////////////////////////

    @Override
    public AbstractSource create_source(Link origin, DemandProfile demand_profile, Commodity commodity, Path path) {
        return new VehicleSource(origin,demand_profile,commodity,path);
    }

    //////////////////////////////////////////////////////////////
    // protected
    //////////////////////////////////////////////////////////////

    protected Map<AbstractLaneGroup,Double> std_lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups) {

        // put the whole packet i the lanegroup with the most space.
        Optional<? extends AbstractLaneGroup> best_lanegroup = candidate_lanegroups.stream()
                .max(Comparator.comparing(AbstractLaneGroup::get_supply_per_lane));

        if(best_lanegroup.isPresent()) {
            Map<AbstractLaneGroup,Double> A = new HashMap<>();
            A.put(best_lanegroup.get(),1d);
            return A;
        } else
            return null;
    }

}
