/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package dispatch;

import error.OTMException;
import output.AbstractOutputTimed;

public class EventTimedWrite extends AbstractEvent {

    public EventTimedWrite(Dispatcher dispatcher,float timestamp,Object obj){
        super(dispatcher,7,timestamp,obj);
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        ((AbstractOutputTimed)recipient).write(timestamp,null);
    }
}
