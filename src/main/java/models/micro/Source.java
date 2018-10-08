/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.micro;

import commodity.Commodity;
import commodity.Path;
import common.Link;
import dispatch.Dispatcher;
import dispatch.EventCreateVehicle;
import error.OTMException;
import profiles.DemandProfile;
import runner.Scenario;

/**
 * Created by gomes on 5/30/2017.
 */
public class Source extends common.AbstractSource {

    public Source(Link link, DemandProfile profile, Commodity commodity, Path path) {
        super(link, profile, commodity, path);
    }

    public void insert_vehicle(float timestamp) throws OTMException {
//        System.out.println(timestamp + "\tinsert_vehicle");
//
//        // create a vehicle
//        Vehicle vehicle= new Vehicle(key,commodity.vehicle_event_listeners);
//
//        // sample its next link according to commodity
//        NextLinkTargetLanegroups x = link.packet_splitter.get_targetlanegroup_nextlink_for_state(vehicle.get_key());
//        vehicle.set_next_link_id(x.next_link_id);
//
//        // choose best one from target lanegroups
//        LaneGroup lanegroup = (LaneGroup) x.target_lanegroups.stream().max(AbstractLaneGroup::compareTo).get();
//
//        // move the vehicle
//        vehicle.move_to_lanegroup(timestamp);

    }

    public void schedule_next_vehicle(Dispatcher dispatcher, float timestamp) {
        System.out.println(timestamp + "\tschedule_next_vehicle");

        if(!link.is_source)
            return;
        Scenario scenario = link.network.scenario;
        Float wait_time = scenario.get_waiting_time(source_demand);
        if(wait_time!=null)
            dispatcher.register_event(new EventCreateVehicle(dispatcher,timestamp+wait_time,this));

    }
}
