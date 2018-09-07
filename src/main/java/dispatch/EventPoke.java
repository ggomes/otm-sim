/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package dispatch;

import error.OTMException;

public class EventPoke extends AbstractEvent {

    public EventPoke(Dispatcher dispatcher, int dispatch_order, float timestamp, Object recipient) {
        super(dispatcher, dispatch_order, timestamp, recipient);
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        ((InterfacePokable)recipient).poke(dispatcher,timestamp);
    }

}
