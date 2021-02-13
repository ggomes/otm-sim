package dispatch;

import error.OTMException;
import profiles.SplitMatrixProfile;
import profiles.TimeMap;

import java.util.Map;

public class EventSplitChange extends AbstractEvent {

    protected Map<Long,Double> outlink2value;

    public EventSplitChange(Dispatcher dispatcher, float timestamp, SplitMatrixProfile splitProfile, Map<Long,Double> outlink2value){
        super(dispatcher,0,timestamp,splitProfile);
        this.outlink2value = outlink2value;
    }

    @Override
    public void action() throws OTMException {
        SplitMatrixProfile smp = (SplitMatrixProfile)recipient;
        smp.set_all_current_splits(outlink2value);
        TimeMap time_map = smp.get_splits().get_change_following(timestamp);
        smp.register_next_change(dispatcher,time_map);
    }

}
