package api.info;

import actuator.AbstractActuator;
import actuator.InterfaceActuatorTarget;

public class ActuatorInfo {

    /** Integer id of the actuator. */
    public long id;

    /** Type of the actuator. */
    public AbstractActuator.Type type;

    public Long target_id;

    public ActuatorInfo(AbstractActuator x){
        this.id = x.id;
        this.type = x.getType();
        this.target_id = x.target==null ? null : x.target.getId();
    }

    public long getId() {
        return id;
    }

    public AbstractActuator.Type getType() {
        return type;
    }

    public Long getTarget_id(){
        return target_id;
    }

    @Override
    public String toString() {
        return "ActuatorInfo{" +
                "id=" + id +
                ", type=" + type +
                '}';
    }

}
