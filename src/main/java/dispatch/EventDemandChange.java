package dispatch;

import common.AbstractDemandGenerator;
import error.OTMException;
import profiles.DemandProfile;

public class EventDemandChange extends AbstractEvent {

    private double demand_vps;

    public EventDemandChange(Dispatcher dispatcher, float timestamp, AbstractDemandGenerator demand_gen, double demand_vps){
        super(dispatcher,0,timestamp,demand_gen);
        this.demand_vps = demand_vps;
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        AbstractDemandGenerator demand_gen= (AbstractDemandGenerator) recipient;
        demand_gen.set_demand_vps(dispatcher,timestamp,demand_vps);
        demand_gen.register_next_change(dispatcher,timestamp);
    }

}
