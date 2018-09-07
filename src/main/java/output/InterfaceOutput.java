/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package output;

import error.OTMException;
import dispatch.Dispatcher;
import runner.RunParameters;

public interface InterfaceOutput {
    String get_output_file();
    void open() throws OTMException;
    void close() throws OTMException;
    void write(float timestamp,Object obj) throws OTMException;
    void register(RunParameters props, Dispatcher dispatcher) throws OTMException;
}
