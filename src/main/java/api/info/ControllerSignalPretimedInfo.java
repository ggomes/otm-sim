package api.info;

import control.sigint.ControllerSignalPretimed;

import java.util.ArrayList;
import java.util.List;

public class ControllerSignalPretimedInfo extends ControllerInfo {

//    public List<ScheduleItemInfo> schedule;

    public ControllerSignalPretimedInfo(ControllerSignalPretimed x){
        super(x);
//        this.schedule = new ArrayList<>();
//        for(ScheduleItem item : x.schedule)
//            this.schedule.add(new ScheduleItemInfo(item));
    }

//    public List<ScheduleItemInfo> getSchedule() {
//        return schedule;
//    }

}
