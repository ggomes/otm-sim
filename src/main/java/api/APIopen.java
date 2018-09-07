/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package api;

import runner.Scenario;

public class APIopen {

    public API api;

    public APIopen(API api){
        this.api = api;
    }

    public Scenario scenario(){
        return api.scenario;
    }

}
