package dispatch;

import error.OTMException;

public class EventPoke extends AbstractEvent {

    public EventPoke(Dispatcher dispatcher, int dispatch_order, float timestamp, Object recipient) {
        super(dispatcher, dispatch_order, timestamp, recipient);
    }

    @Override
    public void action() throws OTMException {
        ((Pokable)recipient).poke(dispatcher,timestamp);
    }

}
