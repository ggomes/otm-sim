/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package plugin;

public class PluginManifest {
    public String clazz;
    public String jarfile;
    public PluginManifest(String clazz, String jarfile) {
        this.clazz = clazz;
        this.jarfile = jarfile;
    }
}
