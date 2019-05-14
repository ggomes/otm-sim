/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package api.info;

import actuator.AbstractActuator;
import actuator.InterfaceActuatorTarget;

public class ActuatorInfo {

    /** Integer id of the actuator. */
    public long id;

    /** Type of the actuator. */
    public AbstractActuator.Type type;

    public InterfaceActuatorTarget target;

    public ActuatorInfo(AbstractActuator x){
        this.id = x.id;
        this.type = x.type;
        this.target = x.target;
    }

    public long getId() {
        return id;
    }

    public AbstractActuator.Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "ActuatorInfo{" +
                "id=" + id +
                ", type=" + type +
                '}';
    }

}
