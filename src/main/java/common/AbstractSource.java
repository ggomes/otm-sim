/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package common;

import commodity.Commodity;
import commodity.Path;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import keys.KeyCommPathOrLink;
import profiles.DemandProfile;

public abstract class AbstractSource {


    // TODO: The child classes for this class are Fluid vs. Vehicle, but could also be
    // Pathfull vs. Pathless. Pathfull can be speeded up by caching the candidate lane groups,
    // which do not change. This is already done in the fluid source.

    public Link link;
    public DemandProfile profile;   // profile that created this source
    public Path path;
    public Commodity commodity;

    // demand value
    protected double source_demand_vps;    // vps

    public AbstractSource(Link link, DemandProfile profile, Commodity commodity, Path path){
        this.link = link;
        this.profile = profile;
        this.commodity = commodity;
        this.path = path;
        this.source_demand_vps = 0f;
    }

    public void delete(){
        link = null;
        profile = null;
        profile = null;
    }

    public void validate(OTMErrorLog errorLog) {
        if(link.is_sink)
            errorLog.addError("source cannot be placed on a sink link.");
    }

    public void set_demand_vps(Dispatcher dispatcher, float time, double vps) throws OTMException {
        source_demand_vps = vps;
    }

    public final KeyCommPathOrLink sample_key(){
        if(commodity.pathfull){
            return new KeyCommPathOrLink(commodity.getId(),path.getId(),true);
        } else {
            Long next_link_id = link.commodity2split.get(commodity.getId()).sample_output_link();
            return new KeyCommPathOrLink(commodity.getId(),next_link_id,false);
        }
    }

}
