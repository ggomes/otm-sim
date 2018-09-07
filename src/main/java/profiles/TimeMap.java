/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package profiles;

import java.util.Map;

public class TimeMap {
    public float time;
    public Map<Long,Double> value;

    public TimeMap(float time,Map<Long,Double> value) {
        this.time = time;
        this.value = value;
    }

}
