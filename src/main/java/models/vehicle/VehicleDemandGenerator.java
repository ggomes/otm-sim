package models.vehicle;

import commodity.Commodity;
import commodity.Path;
import core.AbstractDemandGenerator;
import core.AbstractVehicle;
import core.Link;
import dispatch.Dispatcher;
import dispatch.EventCreateVehicle;
import error.OTMException;
import core.State;
import core.AbstractLaneGroup;
import packet.PacketLaneGroup;
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

        Float wait_time = OTMUtils.get_waiting_time(source_demand_vps,link.model.stochastic_process);
        if(wait_time!=null) {
            EventCreateVehicle new_event = new EventCreateVehicle(dispatcher, timestamp + wait_time, this);
            dispatcher.register_event(new_event);
            vehicle_scheduled = true;
        }
    }

    public void insert_vehicle(float timestamp) throws OTMException {

        AbstractVehicleModel model = (AbstractVehicleModel) link.model;

        // create a vehicle
        AbstractVehicle vehicle = model.create_vehicle(commodity.getId(),commodity.vehicle_event_listeners);

        // sample key
        State key = sample_key();
        vehicle.set_state(key);

        if(commodity.pathfull)
            vehicle.path = path;

        // extract next link
        Long next_link = commodity.pathfull ? link.path2outlink.get(path.getId()).getId() : key.pathOrlink_id;

        // candidate lane groups
        Set<AbstractLaneGroup> candidate_lane_groups = link.outlink2lanegroups.get(next_link);

        // pick from among the eligible lane groups
        AbstractLaneGroup join_lanegroup = link.model.lanegroup_proportions(candidate_lane_groups).keySet().iterator().next();

        // package and add to joinlanegroup
        join_lanegroup.add_vehicle_packet(timestamp,new PacketLaneGroup(vehicle),next_link);

        // this scheduled vehicle has been created
        vehicle_scheduled = false;
    }

}
