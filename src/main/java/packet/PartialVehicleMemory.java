/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package packet;

import keys.KeyCommPathOrLink;
import models.pq.Vehicle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PartialVehicleMemory {

    public Map<KeyCommPathOrLink,Double> remainder;

    ////////////////////////////////////////////////////////////
    // construction
    ////////////////////////////////////////////////////////////

    public PartialVehicleMemory(){
        this.remainder = new HashMap<>();
    }

    ////////////////////////////////////////////////////////////
    // public
    ////////////////////////////////////////////////////////////

    public Set<Vehicle> process_packet(PartialVehicleMemory packet_pvm){

        Set<Vehicle> vehicles = new HashSet<>();

        for(Map.Entry<KeyCommPathOrLink,Double> e : packet_pvm.remainder.entrySet()){
            KeyCommPathOrLink key = e.getKey();
            double value = remainder.containsKey(key) ? remainder.get(key) + e.getValue() : e.getValue();

            if(value>=1d){
                int num_veh = (int) value;
                remainder.put(key,value - num_veh);
                for(int i=0;i<num_veh;i++)
                    vehicles.add(new Vehicle(key,null));
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
