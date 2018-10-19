/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package dispatch;

import error.OTMException;
import models.pq.LaneGroup;

public class EventReleaseVehicleFromLaneGroup extends AbstractEvent {


    public EventReleaseVehicleFromLaneGroup(Dispatcher dispatcher,float timestamp, Object obj) {
        super(dispatcher,4,timestamp,obj);

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
        ((LaneGroup) recipient).release_vehicle_packets(timestamp);
    }

}
