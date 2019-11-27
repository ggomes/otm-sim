package traveltime;

import models.BaseLaneGroup;
import profiles.Profile1D;

public abstract class AbstractLaneGroupTimer {

    public BaseLaneGroup lg;
    public Profile1D travel_time;
    public abstract double get_mean_and_clear();

    public AbstractLaneGroupTimer(BaseLaneGroup lg, float outDt){
        this.lg = lg;
        travel_time = new Profile1D(null, outDt);
    }


}
