/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package packet;

import models.AbstractLaneGroup;

import java.util.Set;

/** Packets of vehicles (micro, meso, and/or macro) passed to a lane group **/

public abstract class AbstractPacketLaneGroup implements InterfacePacketLaneGroup {

    // Vehicles should change lanes into one of these lanegroups
    public Set<AbstractLaneGroup> target_lanegroups;

    public AbstractPacketLaneGroup(){}

    public AbstractPacketLaneGroup(Set<AbstractLaneGroup> target_lanegroups){
        this.target_lanegroups = target_lanegroups;
    }

}
