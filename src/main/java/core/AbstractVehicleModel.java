package core;

import commodity.Commodity;
import commodity.Path;
import error.OTMException;
import models.vehicle.VehicleDemandGenerator;
import profiles.Profile1D;
import utils.OTMUtils;
import utils.StochasticProcess;

import java.util.*;

public abstract class AbstractVehicleModel extends AbstractModel implements InterfaceVehicleModel {

    public AbstractVehicleModel(String name, Set<Link> links, StochasticProcess process) throws OTMException {
        super(AbstractModel.Type.Vehicle,name,links,process);
    }

    //////////////////////////////////////////////////////////////
    // sample implementation
    //////////////////////////////////////////////////////////////

    @Override
    public AbstractDemandGenerator create_source(Link origin, Profile1D profile, Commodity commodity, Path path) {
        return new VehicleDemandGenerator(origin,profile,commodity,path);
    }

    //////////////////////////////////////////////////////////////
    // protected
    //////////////////////////////////////////////////////////////

    protected Map<AbstractLaneGroup,Double> std_lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups) {

        // put the whole core.packet i the lanegroup with the most space.
        Optional<? extends AbstractLaneGroup> best_lanegroup = candidate_lanegroups.stream()
                .max(Comparator.comparing(AbstractLaneGroup::get_supply_per_lane));

        if(best_lanegroup.isPresent()) {
            Map<AbstractLaneGroup,Double> A = new HashMap<>();
            A.put(best_lanegroup.get(),1d);
            return A;
        } else
            return null;
    }

    final public Float get_waiting_time_sec(double rate_vps){
        return OTMUtils.get_waiting_time(rate_vps,stochastic_process);
    }

}
