/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package packet;

import commodity.Commodity;
import keys.KeyCommPathOrLink;
import models.pq.Vehicle;
import runner.Scenario;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PartialVehicleMemory {

    private Map<Long, Commodity> commodities;
    public Map<KeyCommPathOrLink,Double> remainder;

    ////////////////////////////////////////////////////////////
    // construction
    ////////////////////////////////////////////////////////////

    public PartialVehicleMemory(Map<Long, Commodity> commodities){
        this.remainder = new HashMap<>();
        this.commodities = commodities;
    }

    ////////////////////////////////////////////////////////////
    // public
    ////////////////////////////////////////////////////////////

    public Set<Vehicle> process_packet(PartialVehicleMemory packet){

        Set<Vehicle> vehicles = new HashSet<>();

        for(Map.Entry<KeyCommPathOrLink,Double> e : packet.remainder.entrySet()){
            KeyCommPathOrLink key = e.getKey();
            double value = remainder.containsKey(key) ? remainder.get(key) + e.getValue() : e.getValue();

            if(value>=1d){
                int num_veh = (int) value;
                remainder.put(key,value - num_veh);
                for(int i=0;i<num_veh;i++)
                    vehicles.add(new Vehicle(commodities.get(key.commodity_id)));
            }
            else
                remainder.put(key,value);
        }

        return vehicles;
    }

    public double get_value(KeyCommPathOrLink key){
        return remainder.containsKey(key) ? remainder.get(key) : 0d;
    }

    public void set_value(KeyCommPathOrLink key,double val){
        remainder.put(key,val);
    }

}
