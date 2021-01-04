package dispatch;

import control.AbstractController;
import error.OTMException;

public class EventInitializeController extends AbstractEvent {
    public EventInitializeController(Dispatcher dispatcher, float timestamp, Object recipient) {
        super(dispatcher, 35, timestamp, recipient);
    }

    @Override
    public void action() throws OTMException {
        ((AbstractController) this.recipient).initialize(dispatcher.scenario,false);
        ((Pokable) this.recipient).poke(dispatcher,dispatcher.current_time);
    }
}
