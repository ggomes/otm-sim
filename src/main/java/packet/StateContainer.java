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
import models.AbstractLaneGroupVehicles;
import models.AbstractVehicleModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StateContainer {

    public AbstractLaneGroup lg;
    public Map<KeyCommPathOrLink,Double> amount;

    ////////////////////////////////////////////////////////////
    // construction
    ////////////////////////////////////////////////////////////

    public StateContainer(){
        this.amount = new HashMap<>();
    }

    public StateContainer(AbstractLaneGroupVehicles lg){
        this.amount = new HashMap<>();
        this.lg = lg;
    }

    ////////////////////////////////////////////////////////////
    // public
    ////////////////////////////////////////////////////////////

    public Set<AbstractVehicle> process_fluid_packet(FluidLaneGroupPacket packet){

        if(lg==null)
            return null;

        AbstractVehicleModel model = (AbstractVehicleModel) lg.link.model;

        Set<AbstractVehicle> vehicles = new HashSet<>();

        // iterate through all keys ion the packet
        for(Map.Entry<KeyCommPathOrLink,Double> e : packet.state2vehicles.entrySet()){
            KeyCommPathOrLink key = e.getKey();
            double value = amount.containsKey(key) ? amount.get(key) + e.getValue() : e.getValue();

            if(value>=1d){
                int num_veh = (int) value;
                amount.put(key,value - num_veh);
                for(int i=0;i<num_veh;i++)
                    vehicles.add(model.create_vehicle(key.commodity_id,null));
            }
            else
                amount.put(key,value);
        }

        return vehicles;
    }

    public double get_value(KeyCommPathOrLink key){
        return amount.containsKey(key) ? amount.get(key) : 0d;
    }

    public void set_value(KeyCommPathOrLink key,double val){
        amount.put(key,val);
    }

}
