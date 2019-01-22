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
import profiles.DemandProfile;
import runner.Scenario;

import java.util.Collection;
import java.util.HashSet;

public class VehicleSource extends common.AbstractSource {

    private EventCreateVehicle scheduled_vehicle_event;

    public VehicleSource(Link link, DemandProfile profile, Commodity commodity, Path path) {
        super(link,profile,commodity,path);
    }

    @Override
    public void set_demand_vps(Dispatcher dispatcher,float time,double value) throws OTMException {
        super.set_demand_vps(dispatcher,time,value);
        if(value>0)
            schedule_next_vehicle(dispatcher, time);
    }

    public void schedule_next_vehicle(Dispatcher dispatcher, float timestamp){
        if(!link.is_source)
            return;
        Scenario scenario = link.network.scenario;
        Float wait_time = scenario.get_waiting_time_sec(source_demand_vps);
        if(wait_time!=null && scheduled_vehicle_event ==null) {
            EventCreateVehicle new_event = new EventCreateVehicle(dispatcher, timestamp + wait_time, this);
            dispatcher.register_event(new_event);
            scheduled_vehicle_event = new_event;
        }
    }

    public void insert_vehicle(float timestamp) throws OTMException {

        AbstractVehicleModel model = (AbstractVehicleModel) link.model;

        // this scheduled vehicle has been created
        scheduled_vehicle_event = null;

        // create a vehicle
        AbstractVehicle vehicle = model.create_vehicle(key,commodity.vehicle_event_listeners);

        // sample its next link according to commodity
        Collection<AbstractLaneGroup> target_lanegroups;
        Long next_link_id;
        if(link.packet_splitter==null){
            next_link_id = link.end_node.out_links.values().iterator().next().getId();
            target_lanegroups = link.lanegroups_flwdn.values();
        } else {
            next_link_id = link.packet_splitter.get_nextlink_for_key(vehicle.get_key());
            target_lanegroups = next_link_id==null ? link.lanegroups_flwdn.values() :
                    link.packet_splitter.outputlink_targetlanegroups.get(next_link_id);
        }

        // change the state of a pathless vehicle
        vehicle.set_next_link_id(next_link_id);

        // choose best one from target lanegroups
        if(target_lanegroups.isEmpty())
            return;

        // this map will have a single entry
        AbstractLaneGroup joinlanegroup = link.model.lanegroup_proportions(target_lanegroups).keySet().iterator().next();

        VehiclePacket vp = new VehiclePacket(vehicle,new HashSet(target_lanegroups));
        joinlanegroup.add_native_vehicle_packet(timestamp,vp);

//        // move the vehicle
//        vehicle.move_to_queue(timestamp,joinlanegroup.transit_queue);
//
//        // register_initial_events dispatch to go to waiting queue
//        Dispatcher dispatcher = link.network.scenario.dispatcher;
//        dispatcher.register_event(new EventTransitToWaiting(dispatcher,timestamp + joinlanegroup.transit_time_sec,vehicle));
//
//        // inform the travel timers
//        link.travel_timers.forEach(z->z.vehicle_enter(timestamp,vehicle));

    }

}
