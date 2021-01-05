package dispatch;

import traveltime.LinkTravelTimeManager;
import error.OTMException;

public class EventComputeTravelTime extends AbstractEvent  {

    // The object is the list of links where travel time should be computed.
    public EventComputeTravelTime(Dispatcher dispatcher, float timestamp, Object recipient) {

        // Note: dispatch order is 6 so that it happens before times write which is 7
        super(dispatcher, 65, timestamp, recipient);
    }

    @Override
    public void action() throws OTMException {
        ((LinkTravelTimeManager) recipient).run(timestamp);
    }

}
