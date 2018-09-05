package dispatch;

import error.OTMException;
import runner.RunParameters;
import runner.Scenario;

public class EventStopSimulation extends AbstractEvent {

    public EventStopSimulation(Scenario scenario, Dispatcher dispatcher, float timestamp){
        super(dispatcher,3,timestamp,scenario);
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        ((Scenario)recipient).end_run();
        dispatcher.stop();
    }

}
