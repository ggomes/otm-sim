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
import dispatch.Dispatcher;
import dispatch.EventCreateVehicle;
import error.OTMException;
import keys.KeyCommPathOrLink;
import packet.VehicleLaneGroupPacket;
import profiles.DemandProfile;
import runner.Scenario;

import java.util.Collection;
import java.util.HashSet;

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
        AbstractVehicle vehicle = model.create_vehicle(commodity);

        // sample next link and key
        Long outlink_id;
        KeyCommPathOrLink key;
        if(commodity.pathfull){
            Link outlink = link.path2outlink.get(path.getId());
            outlink_id = outlink.getId();
            key = new KeyCommPathOrLink(commodity.getId(),path.getId(),true);
        } else {
            outlink_id = link.sample_nextlink_for_commodity(commodity);
            key = new KeyCommPathOrLink(commodity.getId(),outlink_id,false);
        }
        vehicle.set_key(key);

        // target lane groups
        assert(link.outlink2lanegroups.containsKey(outlink_id));
        Collection<AbstractLaneGroup> target_lanegroups = link.outlink2lanegroups.get(outlink_id);

        // choose best one from target lanegroups
        if(target_lanegroups.isEmpty())
            return;

        // this map will have a single entry
        AbstractLaneGroup joinlanegroup = link.model.lanegroup_proportions(target_lanegroups).keySet().iterator().next();

        // package and add to joinlanegroup
        joinlanegroup.add_vehicle_packet(timestamp,new VehicleLaneGroupPacket(vehicle,new HashSet(target_lanegroups)));

        // this scheduled vehicle has been created
        vehicle_scheduled = false;
    }

}
