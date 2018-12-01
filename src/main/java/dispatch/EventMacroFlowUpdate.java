/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package dispatch;

import error.OTMException;
import common.Network;
import models.AbstractDiscreteTimeModel;

public class EventMacroFlowUpdate extends AbstractEvent {

    public EventMacroFlowUpdate(Dispatcher dispatcher, float timestamp, Object model){
        super(dispatcher,5,timestamp,model);
    }

    @Override
    public void action(boolean verbose) throws OTMException {

        super.action(verbose);

        AbstractDiscreteTimeModel model = (AbstractDiscreteTimeModel)recipient;

        // update the models.ctm state
        model.update_macro_flow(timestamp);

        // register next clock tick
        float next_timestamp = timestamp + model.dt;
        if(next_timestamp<=dispatcher.stop_time)
            dispatcher.register_event(new EventMacroFlowUpdate(dispatcher,next_timestamp,model));
    }

}
