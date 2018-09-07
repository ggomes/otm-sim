/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package tests;

import org.junit.Test;
import runner.OTM;
import utils.OTMUtils;

public class TestVersion {

    @Test
    public void test_get_version(){
        System.out.println("otm-base: " + OTMUtils.getBaseGitHash());
        System.out.println("otm-sim: " + OTM.getGitHash());
    }

}
