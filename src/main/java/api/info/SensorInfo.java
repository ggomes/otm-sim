/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package api.info;

import sensor.AbstractSensor;

public class SensorInfo {

    /** Integer id for this sensor. */
    public long id;

    /** Sample time in seconds. */
    public float dt;

    /** Id of the link where the sensor is located. */
    public long link_id;

    /** Enumeration for the type of sensor. */
    public AbstractSensor.Type type;

    public SensorInfo(AbstractSensor x){
        this.id = x.id;
        this.type = x.type;
        this.dt = x.dt;
//        this.link_id = x.link.getId();
    }

    public long getId() {
        return id;
    }

    public float getDt() {
        return dt;
    }

    public long getLink_id() {
        return link_id;
    }

    public AbstractSensor.Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "SensorInfo{" +
                "id=" + id +
                ", dt=" + dt +
                ", link_id=" + link_id +
                ", type=" + type +
                '}';
    }
}
