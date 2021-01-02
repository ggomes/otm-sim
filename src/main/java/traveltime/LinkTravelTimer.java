package traveltime;

import core.Link;
import core.AbstractLaneGroup;
import core.AbstractFluidModel;
import core.AbstractVehicleModel;

public class LinkTravelTimer {

    public double instantaneous_travel_time;
    public Link link;

    public LinkTravelTimer(Link link,float outDt){
        this.link = link;

        // create FluidLaneGroupTimer
        if( link.get_model() instanceof AbstractFluidModel)
            for(AbstractLaneGroup lg : link.get_lgs())
                lg.travel_timer = new FluidLaneGroupTimer(lg,outDt);

        // create VehicleLaneGroupTimer
        if( link.get_model() instanceof AbstractVehicleModel)
            for(AbstractLaneGroup lg : link.get_lgs())
                lg.travel_timer = new VehicleLaneGroupTimer(lg,outDt);

    }

    public void update_travel_time(){
        instantaneous_travel_time = link.get_lgs().stream()
                .mapToDouble(lg->lg.travel_timer.get_mean_and_clear())
                .average().getAsDouble();
    }

}
