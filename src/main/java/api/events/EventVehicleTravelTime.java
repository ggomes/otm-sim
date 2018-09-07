/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package api.events;

import models.pq.Queue;

public class EventVehicleTravelTime extends AbstractEvent {

    public final long vehicle_id;
    public final Queue from_queue;
    public final Queue to_queue;

    public EventVehicleTravelTime(float timestamp, long vehicle_id, Queue from_queue, Queue to_queue) {
        super(timestamp);
        this.vehicle_id = vehicle_id;
        this.from_queue = from_queue;
        this.to_queue = to_queue;
    }

    @Override
    public String toString() {
        if(from_queue==null)
            return vehicle_id + "\t" + "1";
        if(to_queue==null)
            return vehicle_id + "\t" + "0";
        return "";
    }
}
