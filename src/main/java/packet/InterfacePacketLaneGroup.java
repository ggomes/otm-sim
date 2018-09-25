/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package packet;

import common.AbstractVehicle;
import keys.KeyCommPathOrLink;

public interface InterfacePacketLaneGroup {
    boolean isEmpty();
    void add_link_packet(PacketLink vp);
    void add_macro(KeyCommPathOrLink key, Double vehicles);
    void add_micro(KeyCommPathOrLink key, AbstractVehicle vehicle);
    AbstractPacketLaneGroup times(double x);
}
