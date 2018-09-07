/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package control;

import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Controller;
import runner.Scenario;

public class ControllerRampMeteringAlinea extends AbstractController {

    public ControllerRampMeteringAlinea(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);
    }

    @Override
    public void initialize(Scenario scenario,float now) throws OTMException {
    }

    @Override
    public void register_initial_events(Dispatcher dipatcher) {

    }

    @Override
    public Object get_current_command() {
        return null;
    }

    @Override
    public void update_controller(Dispatcher dispatcher, float timestamp) {

    }

}
