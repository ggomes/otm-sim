/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package packet;

import common.AbstractVehicle;
import keys.KeyCommPathOrLink;
import models.AbstractLaneGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FluidLaneGroupPacket extends AbstractPacketLaneGroup {

    public Map<KeyCommPathOrLink,Double> state2vehicles = new HashMap<>();

    // used by newInstance
    public FluidLaneGroupPacket(){
        super();
    }

    public FluidLaneGroupPacket(Set<AbstractLaneGroup> target_lanegroups){
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
    public AbstractPacketLaneGroup times(double x) {
        FluidLaneGroupPacket z = new FluidLaneGroupPacket(this.target_lanegroups);
        for(Map.Entry<KeyCommPathOrLink,Double> e : state2vehicles.entrySet())
            z.state2vehicles.put(e.getKey(),e.getValue()*x);
        return z;
    }

    @Override
    public boolean isEmpty(){
        return state2vehicles==null || state2vehicles.values().stream().mapToDouble(x->x).sum()==0d;
    }

}
