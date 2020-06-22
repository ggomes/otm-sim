package models.vehicle.spatialq;

import common.InterfaceLaneGroup;
import dispatch.AbstractEvent;
import dispatch.Dispatcher;
import error.OTMException;

public class EventReleaseVehicleFromLaneGroup extends AbstractEvent {

    public EventReleaseVehicleFromLaneGroup(Dispatcher dispatcher, float timestamp, Object obj) {
        super(dispatcher,45,timestamp,obj);

        // add dispatch to vehicle release map if the lanegroup is actuated
        // NOTE: OTHER CONDITIONS FOR CHANGING SATURATION FLOW INCLUDE INCIDENTS
        // WE WILL NEED TO ACCOUNT FOR THIS
//        AbstractLaneGroup lanegroup = (AbstractLaneGroup)obj;
//        if( lanegroup.actuator!=null ){
//            Set<EventReleaseVehicleFromLaneGroup> set;
//            if( dispatcher.vehicle_release_events.containsKey(lanegroup.id) ){
//                set = dispatcher.vehicle_release_events.get(lanegroup.id);
//            }
//            else{
//                set = new HashSet<>();
//                dispatcher.vehicle_release_events.put(lanegroup.id,set);
//            }
//            set.add(this);
//        }
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        ((InterfaceLaneGroup) recipient).release_vehicle_packets(timestamp);
    }

}
