package packet;

import common.AbstractVehicle;
import keys.KeyCommPathOrLink;

public interface InterfacePacketLaneGroup {
    boolean isEmpty();
    void add_link_packet(PacketLink vp);
    void add_macro(KeyCommPathOrLink key, Double vehicles);
    void add_micro(KeyCommPathOrLink key, AbstractVehicle vehicle);
}
