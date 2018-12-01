/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package dispatch;

import common.AbstractSource;
import error.OTMException;
import profiles.DemandProfile;

public class EventDemandChange extends AbstractEvent {

    private double demand_vps;

    public EventDemandChange(Dispatcher dispatcher, float timestamp, DemandProfile demand_profile, double demand_vps){
        super(dispatcher,0,timestamp,demand_profile);
        this.demand_vps = demand_vps;
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        DemandProfile demand_profile = (DemandProfile) recipient;
        AbstractSource source = demand_profile.source;
        if(verbose)
            System.out.println("time=" + timestamp + "\tcommodity_id = " + source.key.commodity_id + " value = " + demand_vps);
        source.set_demand_vps(dispatcher,timestamp,demand_vps);
        demand_profile.register_next_change(dispatcher,timestamp);
    }

}
