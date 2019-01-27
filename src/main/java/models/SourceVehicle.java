/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models;

import commodity.Commodity;
import commodity.Path;
import common.AbstractVehicle;
import common.Link;
import common.RoadConnection;
import dispatch.Dispatcher;
import dispatch.EventCreateVehicle;
import error.OTMException;
import keys.KeyCommPathOrLink;
import packet.VehicleLaneGroupPacket;
import profiles.DemandProfile;
import runner.Scenario;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SourceVehicle extends common.AbstractSource {

    private boolean vehicle_scheduled;

    public SourceVehicle(Link link, DemandProfile profile, Commodity commodity, Path path) {
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
        Scenario scenario = link.network.scenario;
        Float wait_time = scenario.get_waiting_time_sec(source_demand_vps);
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
        KeyCommPathOrLink key = sample_key();
        vehicle.set_key(key);

        // extract next link
        Long next_link = commodity.pathfull ? link.path2outlink.get(path.getId()).getId() : key.pathOrlink_id;

        // candidate lane groups
        Set<AbstractLaneGroup> candidate_lane_groups = link.outlink2lanegroups.get(next_link);

        // pick from among the eligible lane groups
        AbstractLaneGroup join_lanegroup = link.model.lanegroup_proportions(candidate_lane_groups).keySet().iterator().next();

        // package and add to joinlanegroup
        join_lanegroup.add_vehicle_packet(timestamp,new VehicleLaneGroupPacket(vehicle),next_link);

        // this scheduled vehicle has been created
        vehicle_scheduled = false;
    }

}
