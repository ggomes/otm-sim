package models;

import commodity.Commodity;
import commodity.Path;
import common.AbstractSource;
import common.AbstractVehicle;
import common.Link;
import output.InterfaceVehicleListener;
import profiles.DemandProfile;
import utils.StochasticProcess;

import java.util.*;

public abstract class VehicleModel extends BaseModel {

    public VehicleModel(String name, boolean is_default, StochasticProcess process) {
        super(name, is_default,process);
    }

    //////////////////////////////////////////////////
    // factory
    //////////////////////////////////////////////////

    abstract public AbstractVehicle translate_vehicle(AbstractVehicle that);
    abstract public AbstractVehicle create_vehicle(Long comm_id,Set<InterfaceVehicleListener> event_listeners);

    @Override
    public AbstractSource create_source(Link origin, DemandProfile demand_profile, Commodity commodity, Path path) {
        return new VehicleSource(origin,demand_profile,commodity,path);
    }

//    @Override
//    public AbstractPacketLaneGroup create_lanegroup_packet(){
//        return new PacketLaneGroup();
//    }

    //////////////////////////////////////////////////
    // run
    //////////////////////////////////////////////////

    @Override
    public Map<BaseLaneGroup,Double> lanegroup_proportions(Collection<? extends BaseLaneGroup> candidate_lanegroups) {

        // put the whole packet i the lanegroup with the most space.
        Optional<? extends BaseLaneGroup> best_lanegroup = candidate_lanegroups.stream()
                .max(Comparator.comparing(BaseLaneGroup::get_supply_per_lane));

        if(best_lanegroup.isPresent()) {
            Map<BaseLaneGroup,Double> A = new HashMap<>();
            A.put(best_lanegroup.get(),1d);
            return A;
        } else
            return null;
    }
}
