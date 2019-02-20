package models;

import commodity.Commodity;
import commodity.Path;
import common.AbstractSource;
import common.AbstractVehicle;
import common.Link;
import output.InterfaceVehicleListener;
import profiles.DemandProfile;

import java.util.*;

public abstract class AbstractVehicleModel extends AbstractModel {

    public AbstractVehicleModel(String name, boolean is_default) {
        super(name, is_default);
    }

    //////////////////////////////////////////////////
    // factory
    //////////////////////////////////////////////////

    abstract public AbstractVehicle translate_vehicle(AbstractVehicle that);
    abstract public AbstractVehicle create_vehicle(Long comm_id,Set<InterfaceVehicleListener> event_listeners);

    @Override
    public AbstractSource create_source(Link origin, DemandProfile demand_profile, Commodity commodity, Path path) {
        return new SourceVehicle(origin,demand_profile,commodity,path);
    }

//    @Override
//    public AbstractPacketLaneGroup create_lanegroup_packet(){
//        return new PacketLaneGroup();
//    }

    //////////////////////////////////////////////////
    // run
    //////////////////////////////////////////////////

    @Override
    public Map<AbstractLaneGroup,Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups) {

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
