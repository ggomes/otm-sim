package dispatch;

import actuator.sigint.SignalPhase;
import error.OTMException;

public class EventAdvanceSignalPhase extends AbstractEvent {

    public EventAdvanceSignalPhase(Dispatcher dispatcher, float timestamp, SignalPhase phase) {
        super(dispatcher,0, timestamp,phase);
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
//        ((SignalPhase)recipient).execute_next_transition_and_register_following(dispatcher,timestamp);
    }
}
