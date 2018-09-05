package packet;

import common.AbstractLaneGroup;
import common.AbstractVehicle;
import keys.KeyCommPathOrLink;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Packets of vehicles (micro, meso, and/or macro) passed to a lane group **/

public abstract class AbstractPacketLaneGroup implements InterfacePacketLaneGroup {

    // Vehicles should change lanes into one of these lanegroups
    public Set<AbstractLaneGroup> target_lanegroups;

    public AbstractPacketLaneGroup(){}

    public AbstractPacketLaneGroup(Set<AbstractLaneGroup> target_lanegroups, PacketLink vp, boolean in_sink) {
    }

    public AbstractPacketLaneGroup(Set<AbstractLaneGroup> target_lanegroups){
        this.target_lanegroups = target_lanegroups;
    }

}
