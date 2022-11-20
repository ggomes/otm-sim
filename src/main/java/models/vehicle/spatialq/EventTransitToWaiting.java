package models.vehicle.spatialq;

import core.AbstractLaneGroup;
import dispatch.AbstractEvent;
import dispatch.Dispatcher;
import error.OTMException;
import models.vehicle.VehicleLaneGroup;
import models.vehicle.spatialq.MesoLaneGroup;
import models.vehicle.spatialq.MesoVehicle;
import output.InterfaceVehicleListener;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EventTransitToWaiting extends AbstractEvent {

    public EventTransitToWaiting(Dispatcher dispatcher, float timestamp, Object vehicle) {
        super(dispatcher,44,timestamp,vehicle);
    }

    @Override
    public void action() throws OTMException {

        MesoVehicle vehicle = (MesoVehicle)recipient;
        MesoLaneGroup lanegroup = (MesoLaneGroup) vehicle.get_lanegroup();
        Long next_link = vehicle.get_next_link_id();

        if(next_link==null)
            vehicle.waiting_for_lane_change=false;

        // do lane changing
        if(vehicle.waiting_for_lane_change){
            List<AbstractLaneGroup>  lgs = lanegroup.get_link().get_lgs();
            Map<Long,Double> sdf = lgs.stream()
                    .filter(lg -> lg.connects_to_outlink(next_link) )
                    .map(lg -> (MesoLaneGroup) lg)
                    .collect(Collectors.toMap(MesoLaneGroup::getId,
                            MesoLaneGroup::get_waiting_supply) );
            long lgid = Collections.max(sdf.entrySet(), Comparator.comparingDouble(Map.Entry::getValue)).getKey();

            lanegroup = (MesoLaneGroup) lgs.stream().filter(lg->lg.getId()==lgid).findFirst().orElse(null);
            vehicle.waiting_for_lane_change = false;
        }

        // inform listeners
        if(vehicle.get_event_listeners()!=null)
            for(InterfaceVehicleListener ev : vehicle.get_event_listeners())
                ev.move_from_to_queue(timestamp,vehicle,vehicle.my_queue,lanegroup.waiting_queue);

        vehicle.move_to_queue(timestamp,lanegroup.waiting_queue);

    }

}
