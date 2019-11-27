package traveltime;

import models.BaseLaneGroup;

public class FluidLaneGroupTimer extends AbstractLaneGroupTimer {

    private double travel_time_sec;

    public FluidLaneGroupTimer(BaseLaneGroup lg, float outDt) {
        super(lg,outDt);
    }

    public void add_sample(double travel_time_sec){
        this.travel_time_sec = travel_time_sec;
    }

    @Override
    public double get_mean_and_clear(){
        return travel_time_sec;
    }

}
