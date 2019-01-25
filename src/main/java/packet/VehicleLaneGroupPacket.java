/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package packet;

import common.AbstractVehicle;
import common.RoadConnection;
import keys.KeyCommPathOrLink;
import models.AbstractLaneGroup;
import packet.AbstractPacketLaneGroup;
import packet.PacketLink;
import packet.PartialVehicleMemory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class VehicleLaneGroupPacket extends AbstractPacketLaneGroup {

    public Set<AbstractVehicle> vehicles=new HashSet<>();

    // this pvm holds remainders of arriving fluid packets.
    // these remainders are added into the lane group packet
    public PartialVehicleMemory pvm;

    // used by newInstance (dont delete)
    public VehicleLaneGroupPacket(){}

    public VehicleLaneGroupPacket(Set<AbstractVehicle> vehicles, RoadConnection target_road_connection){
        super(target_road_connection);
        this.vehicles = vehicles;
        this.pvm = new PartialVehicleMemory();
    }

    public VehicleLaneGroupPacket(AbstractVehicle vehicle, RoadConnection target_road_connection){
        super(target_road_connection);
        this.vehicles.add(vehicle);
    }

    @Override
    public boolean isEmpty(){
        return vehicles==null || vehicles.isEmpty();
    }

    @Override
    public void add_link_packet(PacketLink vp) {
        if(vp.vehicles!=null)
            vp.vehicles.forEach(v->add_micro(v.get_key(),v));
        if(vp.state2vehicles!=null)
            vp.state2vehicles.forEach( (k,v)->add_macro(k,v));
    }

    @Override
    public void add_macro(KeyCommPathOrLink key, Double value) {
        pvm.set_value(key,pvm.get_value(key) + value);
    }

    @Override
    public void add_micro(KeyCommPathOrLink key, AbstractVehicle vehicle) {
        vehicles.add(vehicle);
    }

    @Override
    public AbstractPacketLaneGroup times(double x) {
        // this should nevere be called!
        return null;
    }

}
