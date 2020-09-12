package dispatch;

import error.OTMException;
import output.OutputLaneGroupAvgVehicles;

public class EventReadLanegroupVehicles extends AbstractEvent {

    public EventReadLanegroupVehicles(Dispatcher dispatcher,float timestamp,Object obj){
        // dispatch order is one less than EventTimedWrite
        super(dispatcher,69,timestamp,obj);
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        OutputLaneGroupAvgVehicles obj = (OutputLaneGroupAvgVehicles)recipient;
        obj.update_total_vehicles(timestamp);
        dispatcher.register_event(new EventReadLanegroupVehicles(dispatcher,timestamp + obj.simDt,recipient));
    }
}
