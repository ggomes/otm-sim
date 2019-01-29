/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package dispatch;

import error.OTMException;
import models.AbstractFluidModel;

public class EventFluidFluxUpdate extends AbstractEvent {

    public EventFluidFluxUpdate(Dispatcher dispatcher, float timestamp, Object model){
        super(dispatcher,5,timestamp,model);
    }

    @Override
    public void action(boolean verbose) throws OTMException {

        super.action(verbose);

        AbstractFluidModel model = (AbstractFluidModel)recipient;

        // update the models.ctm state
        model.update_fluid_flux(timestamp);

        // register next clock tick
        float next_timestamp = timestamp + model.dt;
        if(next_timestamp<=dispatcher.stop_time)
            dispatcher.register_event(new EventFluidFluxUpdate(dispatcher,next_timestamp,model));
    }

}
