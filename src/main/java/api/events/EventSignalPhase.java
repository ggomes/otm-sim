/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package api.events;

import actuator.sigint.BulbColor;

public class EventSignalPhase extends AbstractEvent {

    public final long signal_phase_id;
    public final BulbColor bulbcolor;

    public EventSignalPhase(float timestamp, long signal_phase_id, BulbColor bulbcolor) {
        super(timestamp);
        this.signal_phase_id = signal_phase_id;
        this.bulbcolor = bulbcolor;
    }

    @Override
    public String toString() {
        return new String(signal_phase_id+"\t"+bulbcolor);
    }
}
