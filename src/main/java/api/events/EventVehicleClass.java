/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package api.events;

public class EventVehicleClass extends AbstractEvent {

    public final long vehicle_id;
    public final long commodity_id;

    public EventVehicleClass(long vehicle_id, long commodity_id) {
        super(-1f);
        this.vehicle_id = vehicle_id;
        this.commodity_id = commodity_id;
    }

    @Override
    public String toString() {
        return vehicle_id + "\t"+ commodity_id;
    }

}
