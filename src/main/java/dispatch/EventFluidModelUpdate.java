package dispatch;

import error.OTMException;
import models.fluid.AbstractFluidModel;

public class EventFluidModelUpdate extends AbstractEvent {

    public EventFluidModelUpdate(Dispatcher dispatcher, float timestamp, Object model){
        super(dispatcher,5,timestamp,model);
    }

    @Override
    public void action(boolean verbose) throws OTMException {

        super.action(verbose);

        AbstractFluidModel model = (AbstractFluidModel)recipient;

        // update the models.fluid.ctm state
        model.update_flow(timestamp);

        // register next clock tick
        float next_timestamp = timestamp + model.dt;
        if(next_timestamp<=dispatcher.stop_time)
            dispatcher.register_event(new EventFluidModelUpdate(dispatcher,next_timestamp,model));
    }

}
