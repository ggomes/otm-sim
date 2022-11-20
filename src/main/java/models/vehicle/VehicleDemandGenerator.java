package models.vehicle;

import commodity.Commodity;
import commodity.Path;
import core.*;
import dispatch.Dispatcher;
import dispatch.EventCreateVehicle;
import error.OTMException;
import core.packet.PacketLaneGroup;
import profiles.Profile1D;
import utils.OTMUtils;

import java.util.Set;

public class VehicleDemandGenerator extends AbstractDemandGenerator {

    private boolean vehicle_scheduled;

    public VehicleDemandGenerator(Link link, Profile1D profile, Commodity commodity, Path path) {
        super(link,profile,commodity,path);
        vehicle_scheduled = false;
    }

    @Override
    public void set_demand_vps(Dispatcher dispatcher,float time,double value) throws OTMException {
        super.set_demand_vps(dispatcher,time,value);
        if(value>0)
            schedule_next_vehicle(dispatcher, time);
    }

    public void schedule_next_vehicle(Dispatcher dispatcher, float timestamp){
        if(vehicle_scheduled)
            return;

        Float wait_time = OTMUtils.get_waiting_time(source_demand_vps,link.get_model().stochastic_process);
        if(wait_time!=null) {             ;
            dispatcher.register_event(new EventCreateVehicle(dispatcher, timestamp + wait_time, this));
            vehicle_scheduled = true;
        }
    }

    public void insert_vehicle(float timestamp) throws OTMException {

        AbstractVehicleModel model = (AbstractVehicleModel) link.get_model();

        // create a vehicle
        AbstractVehicle vehicle = model.create_vehicle(commodity.getId(),commodity.vehicle_event_listeners);

        // sample key
        State state = sample_state();
        vehicle.set_state(state);

        if(commodity.pathfull)
            vehicle.path = path;

        // extract next link
        Long next_link = commodity.pathfull ? link.get_next_link_in_path(path.getId()).getId() : state.pathOrlink_id;
        vehicle.set_next_link_id(next_link);

        // candidate lane groups
        Set<AbstractLaneGroup> candidate_lane_groups = link.get_lanegroups_for_outlink(next_link);

        // pick from among the eligible lane groups
        AbstractLaneGroup join_lanegroup = link.get_model().lanegroup_proportions(candidate_lane_groups).keySet().iterator().next();

        // package and add to joinlanegroup
        join_lanegroup.add_vehicle_packet(timestamp,new PacketLaneGroup(vehicle),next_link);

        // this scheduled vehicle has been created
        vehicle_scheduled = false;
    }

}
