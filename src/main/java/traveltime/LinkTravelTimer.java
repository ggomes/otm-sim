package traveltime;

import common.Link;
import models.BaseLaneGroup;
import models.FluidModel;
import models.VehicleModel;

public class LinkTravelTimer {

    public double instantaneous_travel_time;
    public Link link;

    public LinkTravelTimer(Link link,float outDt){
        this.link = link;

        // create FluidLaneGroupTimer
        if( link.model instanceof FluidModel)
            for(BaseLaneGroup lg : link.lanegroups_flwdn.values())
                lg.travel_timer = new FluidLaneGroupTimer(lg,outDt);

        // create VehicleLaneGroupTimer
        if( link.model instanceof VehicleModel)
            for(BaseLaneGroup lg : link.lanegroups_flwdn.values())
                lg.travel_timer = new VehicleLaneGroupTimer(lg,outDt);

    }

    public void update_travel_time(){
        instantaneous_travel_time = link.lanegroups_flwdn.values().stream()
                .mapToDouble(lg->lg.travel_timer.get_mean_and_clear())
                .average().getAsDouble();
    }

}
