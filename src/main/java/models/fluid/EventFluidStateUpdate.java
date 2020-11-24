package models.fluid;

import dispatch.AbstractEvent;
import dispatch.Dispatcher;
import error.OTMException;

public class EventFluidStateUpdate extends AbstractEvent {

    public EventFluidStateUpdate(Dispatcher dispatcher, float timestamp, Object model){
        super(dispatcher,55,timestamp,model);
    }

    @Override
    public void action() throws OTMException {
        AbstractFluidModel model = (AbstractFluidModel)recipient;

        // update the models.fluid.ctm state
        model.update_fluid_state(timestamp);

        // register next clock tick
        dispatcher.register_event(new EventFluidStateUpdate(dispatcher,timestamp + model.dt_sec,model));
    }

}
