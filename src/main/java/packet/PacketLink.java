package packet;

import common.AbstractLaneGroup;
import common.AbstractVehicle;
import keys.KeyCommPathOrLink;
import models.pq.Vehicle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Packets of vehicles (micro, meso, and/or macro) passed to a link **/

public class PacketLink {

    // The packet arrives to a set of lanegroups
    public Set<AbstractLaneGroup> arrive_to_lanegroups;
    public Set<AbstractVehicle> vehicles;
    public Map<KeyCommPathOrLink,Double> state2vehicles;

    // The packet may come with one of these. It is a default split among the
    // arrive_lane_groups. It is computed by the macro node model, and known to be a feasible solution
    // for splitting the packet so that the pieces all fit into their respective lanegroups. However
    // it is computed prior to branding. So after branding, if there is a better arrangement that is also
    // feasible, then that can be used.
    // If it is null, then there is no recommendation (probably because it is a micro or meso model)
//    public Map<Long,Double> arrive_split;       // lgid -> split

    // empty constructor
    public PacketLink(Set<AbstractLaneGroup> arrive_to_lanegroups){
        this.arrive_to_lanegroups = arrive_to_lanegroups;
        this.state2vehicles = new HashMap<>();
        this.vehicles = new HashSet<>();
    }

    // macro constructor
    public PacketLink(Map<KeyCommPathOrLink,Double> state2vehicles, Set<AbstractLaneGroup> arrive_to_lanegroups){
        this.arrive_to_lanegroups = arrive_to_lanegroups;
        this.state2vehicles = state2vehicles;
    }

    // single vehicle constructor
    public PacketLink(Vehicle vehicle, Set<AbstractLaneGroup> arrive_to_lanegroups){
        this.arrive_to_lanegroups = arrive_to_lanegroups;
        this.vehicles = new HashSet<>();
        this.vehicles.add(vehicle);
    }

//    public void set_nextlink_id(long nextlink_id){
//
//        if(vehicles!=null)
//            for(AbstractVehicle vehicle : vehicles)
//                vehicle.set_next_link_id(nextlink_id);
//
//        if(state2vehicles!=null){
//            Map<KeyCommPathOrLink,Double> new_state2vehicles = new HashMap<>();
//            for(Map.Entry<KeyCommPathOrLink,Double> e : state2vehicles.entrySet()){
//                KeyCommPathOrLink key = e.getKey();
//                new_state2vehicles.put(
//                        key.isPath ? key : new KeyCommPathOrLink(key.commodity_id,nextlink_id,false),
//                        e.getValue() );
//            }
//            this.state2vehicles = new_state2vehicles;
////            this.arrive_split = null;
//        }
//    }

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
