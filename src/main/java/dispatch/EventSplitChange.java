/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package dispatch;

import error.OTMException;
import profiles.SplitMatrixProfile;

import java.util.Map;

public class EventSplitChange extends AbstractEvent {

    protected SplitMatrixProfile splitProfile;
    protected Map<Long,Double> outlink2value;

    public EventSplitChange(Dispatcher dispatcher, float timestamp, SplitMatrixProfile splitProfile, Map<Long,Double> outlink2value){
        super(dispatcher,0,timestamp,splitProfile.node);
        this.splitProfile = splitProfile;
        this.outlink2value = outlink2value;
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        long commodity_id = splitProfile.commodity_id;
        long linkinid = splitProfile.link_in_id;
        if(verbose) {
            for(Map.Entry e : outlink2value.entrySet())
                System.out.println("\t\toutlinkid = " + e.getKey() + " , value=" + e.getValue());
        }
        ((common.Node)recipient).set_node_split(commodity_id,linkinid,outlink2value);
        splitProfile.register_next_change(dispatcher,timestamp);
    }

}
