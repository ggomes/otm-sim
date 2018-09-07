/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package api.info;

import geometry.Gate;

public class GateInfo {

    /** Starting position of the gate measured in meters from thee upstream
     * boundary of the link. */
    public float start_pos;

    /** Ending position of the gate measured in meters from thee upstream
     * boundary of the link. */
    public float end_pos;

    public GateInfo(Gate x){
        this.start_pos = x.start_pos;
        this.end_pos = x.end_pos;
    }

    public float getStart_pos() {
        return start_pos;
    }

    public float getEnd_pos() {
        return end_pos;
    }

    @Override
    public String toString() {
        return "GateInfo{" +
                "start_pos=" + start_pos +
                ", end_pos=" + end_pos +
                '}';
    }
}
