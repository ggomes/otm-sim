package dispatch;

import error.OTMException;
import models.fluid.FluidModel;

public class EventFluidStateUpdate extends AbstractEvent  {

    public EventFluidStateUpdate(Dispatcher dispatcher, float timestamp, Object model){
        super(dispatcher,6,timestamp,model);
    }

    @Override
    public void action(boolean verbose) throws OTMException {

        super.action(verbose);

        FluidModel model = (FluidModel)recipient;

        // update the models.fluid.ctm state
        model.update_fluid_state(timestamp);

        // register next clock tick
        float next_timestamp = timestamp + model.dt;
        if(next_timestamp<=dispatcher.stop_time)
            dispatcher.register_event(new EventFluidStateUpdate(dispatcher,next_timestamp,model));
    }

}
