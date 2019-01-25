/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package packet;

import common.RoadConnection;
import common.AbstractVehicle;
import keys.KeyCommPathOrLink;
import models.pq.Vehicle;

import java.util.*;

/** Packets of vehicles (micro, meso, and/or macro) passed to a link **/

public class PacketLink {

    // The packet arrives to a set of lanegroups
    public RoadConnection road_connection;
    public Set<AbstractVehicle> vehicles;
    public Map<KeyCommPathOrLink,Double> state2vehicles;

    // empty constructor
    public PacketLink(RoadConnection road_connection){
        this.road_connection = road_connection;
        this.state2vehicles = new HashMap<>();
        this.vehicles = new HashSet<>();
    }

    // macro constructor
    public PacketLink(Map<KeyCommPathOrLink,Double> state2vehicles, RoadConnection road_connection){
        this.road_connection = road_connection;
        this.state2vehicles = state2vehicles;
    }

    // single vehicle constructor
    public PacketLink(Vehicle vehicle,RoadConnection road_connection){
        this.road_connection = road_connection;
        this.vehicles = new HashSet<>();
        this.vehicles.add(vehicle);
    }


    public boolean isEmpty(){
        return no_macro() && no_micro();
    }

    public boolean no_micro(){
        return vehicles==null || vehicles.isEmpty();
    }

    public boolean no_macro(){
        return state2vehicles==null || state2vehicles.values().stream().mapToDouble(x->x).sum()==0d;
    }

}
