/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package api.events;

public class EventControllerScheduleTransition extends AbstractEvent {

    public final long controller_id;
    public final int current_item;

    public EventControllerScheduleTransition(float timestamp, long controller_id, int current_item) {
        super(timestamp);
        this.controller_id = controller_id;
        this.current_item = current_item;
    }

    @Override
    public String toString() {
        return new String(controller_id +"\t" + current_item);
    }
}
