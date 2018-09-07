/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
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
