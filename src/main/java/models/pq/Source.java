/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.pq;

import commodity.Commodity;
import commodity.Path;
import common.*;
import dispatch.Dispatcher;
import dispatch.EventCreateVehicle;
import dispatch.EventTransitToWaiting;
import error.OTMException;
import profiles.DemandProfile;
import runner.Scenario;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

public class Source extends common.AbstractSource {

    EventCreateVehicle scheduled_vehicle_event;

    public Source(Link link, DemandProfile profile, Commodity commodity, Path path) {
        super(link,profile,commodity,path);
    }

    @Override
    public void set_demand_in_veh_per_timestep(Dispatcher dispatcher,float time,double value) throws OTMException {
        super.set_demand_in_veh_per_timestep(dispatcher,time,value);
        if(value>0)
            schedule_next_vehicle(dispatcher, time);
    }

    public void schedule_next_vehicle(Dispatcher dispatcher, float timestamp){
        if(!link.is_source)
            return;
        Scenario scenario = link.network.scenario;
        Float wait_time = scenario.get_waiting_time(source_demand);
        if(wait_time!=null && scheduled_vehicle_event ==null) {
            EventCreateVehicle new_event = new EventCreateVehicle(dispatcher, timestamp + wait_time, this);
            dispatcher.register_event(new_event);
            scheduled_vehicle_event = new_event;
        }
    }

    public void insert_vehicle(float timestamp) throws OTMException {

        // this scheduled vehicle has been created
        scheduled_vehicle_event = null;

        // create a vehicle
        Vehicle vehicle= new Vehicle(key,commodity.vehicle_event_listeners);

        // sample its next link according to commodity
        Collection<AbstractLaneGroup> target_lanegroups;
        Long next_link_id;
        if(link.packet_splitter==null){
            next_link_id = link.end_node.out_links.values().iterator().next().getId();
            target_lanegroups = link.lanegroups.values();
        } else {
            next_link_id = link.packet_splitter.get_nextlink_for_key(vehicle.get_key());
            target_lanegroups = next_link_id==null ? link.lanegroups.values() :
                    link.packet_splitter.outputlink_targetlanegroups.get(next_link_id);
        }

        // change the state of a pathless vehicle
        vehicle.set_next_link_id(next_link_id);

        // choose best one from target lanegroups
        if(target_lanegroups.isEmpty())
            return;

        // I need a linkModel object to call lanegroup_proportions on
        // Annoyingly, Java does not allow overriding of static methods.
        models.pq.LinkModel linkModel = (models.pq.LinkModel) target_lanegroups.iterator().next().link.model;

        // this map will have a single entry
        LaneGroup joinlanegroup = (LaneGroup) linkModel.lanegroup_proportions(target_lanegroups).keySet().iterator().next();

        // move the vehicle
        vehicle.move_to_queue(timestamp,joinlanegroup.transit_queue);

        // register_initial_events dispatch to go to waiting queue
        Dispatcher dispatcher = link.network.scenario.dispatcher;
        dispatcher.register_event(new EventTransitToWaiting(dispatcher,timestamp + joinlanegroup.transit_time_sec,vehicle));

        // inform the travel timers
        link.travel_timers.forEach(z->z.vehicle_enter(timestamp,vehicle));

    }

}
