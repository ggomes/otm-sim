package models.micro;

import common.AbstractVehicle;
import keys.KeyCommPathOrLink;
import packet.AbstractPacketLaneGroup;
import packet.PacketLink;
import packet.PartialVehicleMemory;

import java.util.HashSet;
import java.util.Set;

public class PacketLaneGroup extends AbstractPacketLaneGroup {

    public Set<AbstractVehicle> vehicles=new HashSet<>();
    public PartialVehicleMemory pvm = new PartialVehicleMemory();

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void add_link_packet(PacketLink vp) {

    }

    @Override
    public void add_macro(KeyCommPathOrLink key, Double vehicles) {

    }

    @Override
    public void add_micro(KeyCommPathOrLink key, AbstractVehicle vehicle) {

    }

    @Override
    public AbstractPacketLaneGroup times(double x) {
        return null;
    }
}
