/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package tests;

import org.junit.runners.Parameterized;
import xml.JaxbLoader;

import java.util.*;

public abstract class AbstractTest {

    protected static String output_folder = "temp";

    private static List<String> all_runparams;
    static {
        all_runparams = new ArrayList<>();
        all_runparams.add("data/props/0_5_3600.properties");
    }

    @Parameterized.Parameters
    public static Collection getConfigs(){
        ArrayList<String []> x = new ArrayList<>();
        for(String s : JaxbLoader.get_test_config_names())
            x.add(new String[]{s});
        return x;
    }

}
