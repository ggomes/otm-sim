/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package api.info;

import common.Link;
import common.Point;

public class PointInfo {

    /** X flwdir. Unspecified units. This is not used by the simulator. */
    public float x;

    /** Y flwdir. Unspecified units. This is not used by the simulator. */
    public float y;

    public PointInfo(Point p){
        this.x = p.x;
        this.y = p.y;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    @Override
    public String toString() {
        return "PointInfo{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
