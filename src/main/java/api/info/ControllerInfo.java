package api.info;

import control.AbstractController;

import java.util.ArrayList;
import java.util.List;

public class ControllerInfo {

    /** Integer id of the myController. */
    public long id;

    /** Enumeration for the control algorithm */
    public AbstractController.Algorithm type;

    /** List of integer ids of the actuators for this myController. */
    public List<Long> actuators_ids;

    /** Controller samples/update time in seconds. */
    public Float dt;

    // additional information for specific controllers

    /** Additional information for pretimed signal controllers. */
    ControllerSignalPretimedInfo pretimed_signal_info;

    public ControllerInfo(AbstractController x){
        this.id = x.id;
        this.type = x.type;
        this.dt = Float.isNaN(x.dt) ? Float.NaN : x.dt;
        this.actuators_ids = new ArrayList<>();
        x.actuators.forEach(a->actuators_ids.add(a.id));

//        if(x.myType== AbstractController.Algorithm.SIG_Pretimed)
//            pretimed_signal_info = new ControllerSignalPretimedInfo((_ControllerSignalPretimedNEMA)x);
    }

    public long getId() {
        return id;
    }

    public AbstractController.Algorithm getType() {
        return type;
    }

    public List<Long> getActuators_ids() {
        return actuators_ids;
    }

    public Float getDt() {
        return dt;
    }

    public ControllerSignalPretimedInfo getPretimed_signal_info() {
        return pretimed_signal_info;
    }

    @Override
    public String toString() {
        return "ControllerInfo{" +
                "id=" + id +
                ", myType=" + type +
                ", actuators_ids=" + actuators_ids +
                ", dt=" + dt +
                ", pretimed_signal_info=" + pretimed_signal_info +
                '}';
    }
}
