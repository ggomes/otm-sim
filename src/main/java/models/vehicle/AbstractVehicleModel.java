package models.vehicle;

import commodity.Commodity;
import commodity.Path;
import common.AbstractSource;
import common.AbstractVehicle;
import common.Link;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import geometry.FlowPosition;
import geometry.Side;
import jaxb.OutputRequest;
import models.AbstractLaneGroup;
import models.AbstractModel;
import output.AbstractOutput;
import output.InterfaceVehicleListener;
import output.animation.AbstractLinkInfo;
import profiles.DemandProfile;
import runner.Scenario;
import utils.StochasticProcess;

import java.util.*;

public abstract class AbstractVehicleModel extends AbstractModel implements InterfaceVehicleModel {

    public AbstractVehicleModel(String name, boolean is_default, StochasticProcess process) {
        super(AbstractModel.Type.Vehicle,name, is_default,process);
    }

    //////////////////////////////////////////////////////////////
    // abstract in AbstractModel
    //////////////////////////////////////////////////////////////

    // NOTE: register_with_dispatcher is implemented at the concrete level

    @Override
    public void reset(Link link){ }

    @Override
    public void build() throws OTMException { }

    @Override
    public final AbstractSource create_source(Link origin, DemandProfile demand_profile, Commodity commodity, Path path) {
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
