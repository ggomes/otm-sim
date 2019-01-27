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

import java.util.Set;

/** Flux packet passed to a lane group **/

public abstract class AbstractPacketLaneGroup {


    public AbstractPacketLaneGroup(){}

    abstract public boolean isEmpty();
    abstract public void add_link_packet(PacketLink vp);
    abstract public void add_fluid(KeyCommPathOrLink key, Double vehicles);
    abstract public void add_vehicle(KeyCommPathOrLink key, AbstractVehicle vehicle);
    abstract public AbstractPacketLaneGroup times(double x);

}
