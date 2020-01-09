package traveltime;

import common.Link;
import models.AbstractLaneGroup;
import models.fluid.FluidModel;
import models.vehicle.VehicleModel;

public class LinkTravelTimer {

    public double instantaneous_travel_time;
    public Link link;

    public LinkTravelTimer(Link link,float outDt){
        this.link = link;

        // create FluidLaneGroupTimer
        if( link.model instanceof FluidModel)
            for(AbstractLaneGroup lg : link.lanegroups_flwdn.values())
                lg.travel_timer = new FluidLaneGroupTimer(lg,outDt);

        // create VehicleLaneGroupTimer
        if( link.model instanceof VehicleModel)
            for(AbstractLaneGroup lg : link.lanegroups_flwdn.values())
                lg.travel_timer = new VehicleLaneGroupTimer(lg,outDt);

    }

    public void update_travel_time(){
        instantaneous_travel_time = link.lanegroups_flwdn.values().stream()
                .mapToDouble(lg->lg.travel_timer.get_mean_and_clear())
                .average().getAsDouble();
    }

}
