/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package plugin;

import control.AbstractController;
import error.OTMException;
import jaxb.Controller;
import runner.Scenario;

public abstract class PluginController extends AbstractController {

    public PluginController(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);
    }

}
