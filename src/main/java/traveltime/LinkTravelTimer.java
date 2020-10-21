package traveltime;

import common.Link;
import common.AbstractLaneGroup;
import models.fluid.AbstractFluidModel;
import models.vehicle.AbstractVehicleModel;

public class LinkTravelTimer {

    public double instantaneous_travel_time;
    public Link link;

    public LinkTravelTimer(Link link,float outDt){
        this.link = link;

        // create FluidLaneGroupTimer
        if( link.model instanceof AbstractFluidModel)
            for(AbstractLaneGroup lg : link.lanegroups_flwdn)
                lg.travel_timer = new FluidLaneGroupTimer(lg,outDt);

        // create VehicleLaneGroupTimer
        if( link.model instanceof AbstractVehicleModel)
            for(AbstractLaneGroup lg : link.lanegroups_flwdn)
                lg.travel_timer = new VehicleLaneGroupTimer(lg,outDt);

    }

    public void update_travel_time(){
        instantaneous_travel_time = link.lanegroups_flwdn.stream()
                .mapToDouble(lg->lg.travel_timer.get_mean_and_clear())
                .average().getAsDouble();
    }

}
