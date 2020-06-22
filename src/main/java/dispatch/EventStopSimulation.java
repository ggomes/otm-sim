package dispatch;

import error.OTMException;
import common.Scenario;

public class EventStopSimulation extends AbstractEvent {

    public EventStopSimulation(Scenario scenario, Dispatcher dispatcher, float timestamp){
        super(dispatcher,100,timestamp,scenario);
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        ((Scenario)recipient).end_run();
        dispatcher.stop();
    }

}
