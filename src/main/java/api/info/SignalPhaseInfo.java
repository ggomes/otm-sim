/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package api.info;

import actuator.sigint.SignalPhase;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class SignalPhaseInfo {

    public long id;
    public List<Long> road_connections;
    public float yellow_time;
    public float red_clear_time;
    public float min_green_time;

    public SignalPhaseInfo(SignalPhase x){
        this.id = x.id;
        this.yellow_time = x.yellow_time;
        this.red_clear_time = x.red_clear_time;
        this.min_green_time = x.min_green_time;
        this.road_connections = x.road_connections.stream().map(z->z.getId()).collect(toList());
    }

    public long getId() {
        return id;
    }

    public List<Long> getRoad_connections() {
        return road_connections;
    }

    public float getYellow_time() {
        return yellow_time;
    }

    public float getRed_clear_time() {
        return red_clear_time;
    }

    public float getMin_green_time() {
        return min_green_time;
    }

    @Override
    public String toString() {
        return "SignalPhaseInfo{" +
                "id=" + id +
                ", road_connections=" + road_connections +
                ", yellow_time=" + yellow_time +
                ", red_clear_time=" + red_clear_time +
                ", min_green_time=" + min_green_time +
                '}';
    }

}
