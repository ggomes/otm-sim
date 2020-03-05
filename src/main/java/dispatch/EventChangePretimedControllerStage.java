package dispatch;

import error.OTMException;

public class EventChangePretimedControllerStage extends AbstractEvent {

    public EventChangePretimedControllerStage(Dispatcher dispatcher, int dispatch_order, float timestamp, Object recipient) {
        super(dispatcher, dispatch_order, timestamp, recipient);
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
//        ((ControllerSignalPretimed)recipient).execute_next_transition_and_register_following(dispatcher,timestamp);
    }
}
