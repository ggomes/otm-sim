package core.packet;

import core.RoadConnection;
import core.AbstractVehicle;
import core.State;

import java.util.*;

/** Packets of vehicles (micro, meso, and/or macro) passed to a link **/

public class PacketLink {

    // The core.packet arrives to a set of lanegroups
    public RoadConnection road_connection;
    public Set<AbstractVehicle> vehicles;
    public Map<State,Double> state2vehicles;

    // empty constructor
    public PacketLink(RoadConnection road_connection){
        this.road_connection = road_connection;
        this.state2vehicles = new HashMap<>();
        this.vehicles = new HashSet<>();
    }

    // macro constructor
    public PacketLink(Map<State,Double> state2vehicles, RoadConnection road_connection){
        this.road_connection = road_connection;
        this.state2vehicles = state2vehicles;
    }

    // single vehicle constructor
    public PacketLink(AbstractVehicle vehicle,RoadConnection road_connection){
        this.road_connection = road_connection;
        this.vehicles = new HashSet<>();
        this.vehicles.add(vehicle);
    }

    public double total_macro_vehicles(){
        return state2vehicles.values().stream().mapToDouble(x->x).sum();
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
