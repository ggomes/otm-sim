/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package packet;

import common.AbstractVehicle;
import keys.KeyCommPathOrLink;

import java.util.HashSet;
import java.util.Set;

public class VehicleLaneGroupPacket extends AbstractPacketLaneGroup {

    public Set<AbstractVehicle> vehicles=new HashSet<>();

    // this container holds remainders of arriving fluid packets.
    // these remainders are added into the lane group packet
    public StateContainer container;

    // used by newInstance (dont delete)
    public VehicleLaneGroupPacket(){}

    public VehicleLaneGroupPacket(Set<AbstractVehicle> vehicles){
        super();
        this.vehicles.addAll(vehicles);
        this.container = new StateContainer();
    }

    public VehicleLaneGroupPacket(AbstractVehicle vehicle){
        super();
        this.vehicles.add(vehicle);
    }

    @Override
    public boolean isEmpty(){
        return vehicles==null || vehicles.isEmpty();
    }

    @Override
    public void add_link_packet(PacketLink vp) {
        if(vp.vehicles!=null)
            vp.vehicles.forEach(v-> add_vehicle(v.get_key(),v));
        if(vp.state2vehicles!=null)
            vp.state2vehicles.forEach( (k,v)-> add_fluid(k,v));
    }

    @Override
    public void add_fluid(KeyCommPathOrLink key, Double value) {
        container.set_value(key, container.get_value(key) + value);
    }

    @Override
    public void add_vehicle(KeyCommPathOrLink key, AbstractVehicle vehicle) {
        vehicles.add(vehicle);
    }

    @Override
    public AbstractPacketLaneGroup times(double x) {
        // this should nevere be called!
        return null;
    }

}
