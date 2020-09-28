package dispatch;

import common.InterfaceScenarioElement;
import error.OTMException;

public class EventInitializeController extends AbstractEvent {
    public EventInitializeController(Dispatcher dispatcher, float timestamp, Object recipient) {
        super(dispatcher, 35, timestamp, recipient);
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        ((InterfaceScenarioElement) this.recipient).initialize(dispatcher.scenario);
    }
}
