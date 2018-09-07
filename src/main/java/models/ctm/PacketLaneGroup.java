/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.ctm;

import common.AbstractLaneGroup;
import common.AbstractVehicle;
import keys.KeyCommPathOrLink;
import packet.AbstractPacketLaneGroup;
import packet.PacketLink;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PacketLaneGroup extends AbstractPacketLaneGroup {

    public Map<KeyCommPathOrLink,Double> state2vehicles = new HashMap<>();

    public PacketLaneGroup(){}

    public PacketLaneGroup(Set<AbstractLaneGroup> target_lanegroups){
        super(target_lanegroups);
    }

    @Override
    public void add_link_packet(PacketLink vp) {

        // process macro state
        for (Map.Entry<KeyCommPathOrLink, Double> e : vp.state2vehicles.entrySet()) {
            KeyCommPathOrLink key = e.getKey();
            Double value = e.getValue();
            if(this.state2vehicles.containsKey(key)){
                this.state2vehicles.put(key,this.state2vehicles.get(key)+value);
            } else {
                this.state2vehicles.put(key,value);
            }
        }

        // process micro state
        for(AbstractVehicle vehicle : vp.vehicles ) {
            KeyCommPathOrLink key = vehicle.get_key();
            if(state2vehicles.keySet().contains(key))
                state2vehicles.put(key, state2vehicles.get(key) + 1d);
            else
                state2vehicles.put(key, 1d);
        }

    }

    @Override
    public void add_macro(KeyCommPathOrLink key,Double vehicles){
        if(state2vehicles.containsKey(key))
            state2vehicles.put(key,state2vehicles.get(key)+vehicles);
        else
            state2vehicles.put(key,vehicles);
    }

    @Override
    public void add_micro(KeyCommPathOrLink key, AbstractVehicle vehicle) {
        if(state2vehicles.containsKey(key))
            state2vehicles.put(key,state2vehicles.get(key)+1d);
        else
            state2vehicles.put(key,1d);
    }

    @Override
    public boolean isEmpty(){
        return state2vehicles==null || state2vehicles.values().stream().mapToDouble(x->x).sum()==0d;
    }

}
