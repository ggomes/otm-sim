/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package error;

public class OTMError {
    public enum Level {Warning,Error}
    public String description;
    public Level level;
    public OTMError(String description, Level level){
        this.description = description;
        this.level = level;
    }
}