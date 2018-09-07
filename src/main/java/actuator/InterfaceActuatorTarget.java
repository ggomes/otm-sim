/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package actuator;

import error.OTMException;
import runner.InterfaceScenarioElement;

public interface InterfaceActuatorTarget extends InterfaceScenarioElement {
    void register_actuator(AbstractActuator act) throws OTMException;
}
