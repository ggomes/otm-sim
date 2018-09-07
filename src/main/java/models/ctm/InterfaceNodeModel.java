/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.ctm;

import error.OTMErrorLog;
import error.OTMException;
import runner.Scenario;

public interface InterfaceNodeModel {

    void validate(OTMErrorLog errorLog);

    void initialize(Scenario scenario) throws OTMException ;

//    void exchange_packets(float timestamp) throws OTMException;

    void update_flow(float timestamp,boolean is_sink);

}
