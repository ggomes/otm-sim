/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package dispatch;

import actuator.sigint.SignalPhase;
import error.OTMException;

public class EventAdvanceSignalPhase extends AbstractEvent {

    public EventAdvanceSignalPhase(Dispatcher dispatcher, float timestamp, SignalPhase phase) {
        super(dispatcher,0, timestamp,phase);
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        ((SignalPhase)recipient).execute_next_transition_and_register_following(dispatcher,timestamp);
    }
}
