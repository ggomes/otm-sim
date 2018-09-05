package dispatch;

import common.AbstractSource;
import error.OTMException;
import profiles.DemandProfile;

public class EventDemandChange extends AbstractEvent {

    protected double demand_veh_per_timestep;

    public EventDemandChange(Dispatcher dispatcher, float timestamp, DemandProfile demand_profile, double demand_vps){
        super(dispatcher,0,timestamp,demand_profile);
        this.demand_veh_per_timestep = demand_vps*dispatcher.scenario.sim_dt;
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        DemandProfile demand_profile = (DemandProfile) recipient;
        AbstractSource source = demand_profile.source;
        if(verbose)
            System.out.println("time=" + timestamp + "\tcommodity_id = " + source.key.commodity_id + " value = " + demand_veh_per_timestep);
        source.set_demand_in_veh_per_timestep(dispatcher,timestamp,demand_veh_per_timestep);
        demand_profile.register_next_change(dispatcher,timestamp);
    }

}
