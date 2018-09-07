/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package actuator;

import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import runner.Scenario;

public class ActuatorVSL extends AbstractActuator {

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ActuatorVSL(Scenario scenario, jaxb.Actuator jaxb_actuator) throws OTMException {
        super(scenario,jaxb_actuator);
        System.err.println("ActuatorVSL is not implemented");
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
    }

    @Override
    public void process_controller_command(Object command, Dispatcher dispatcher, float timestamp) {

    }
}
