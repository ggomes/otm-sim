package models.fluid;

import dispatch.AbstractEvent;
import dispatch.Dispatcher;
import error.OTMException;

public class EventFluidModelUpdate extends AbstractEvent {

    public EventFluidModelUpdate(Dispatcher dispatcher, float timestamp, Object model){
        super(dispatcher,50,timestamp,model);
    }

    @Override
    public void action(boolean verbose) throws OTMException {

        super.action(verbose);

        AbstractFluidModel model = (AbstractFluidModel)recipient;

        // update the models.fluid.ctm state
        model.update_flow(timestamp);

        // register next clock tick
        float next_timestamp = timestamp + model.dt_sec;
        if(next_timestamp<=dispatcher.stop_time)
            dispatcher.register_event(new EventFluidModelUpdate(dispatcher,next_timestamp,model));
    }

}
