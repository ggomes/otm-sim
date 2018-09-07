/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package profiles;

import commodity.Commodity;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import keys.DemandType;
import runner.Scenario;
import utils.OTMUtils;

public class DemandProfileOD extends AbstractDemandProfile {

    public Long origin_node;
    public Long destination_node;

    ////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public DemandProfileOD(jaxb.Demand jd,Commodity comm, Long origin_node,Long destination_node) {

        this.commodity = comm;
        this.origin_node = origin_node;
        this.destination_node = destination_node;

        profile = new Profile1D(jd.getStartTime(), jd.getDt(), OTMUtils.csv2list(jd.getContent()));
        profile.multiply(1.0/3600.0);
    }

    ////////////////////////////////////////////
    // implementation
    ///////////////////////////////////////////

    @Override
    public void validate(OTMErrorLog errorLog) {
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {

    }

    @Override
    public void register_initial_events(Dispatcher dispatcher){
    }

    @Override
    public DemandType get_type() {
        return DemandType.pathfull;
    }

    @Override
    public Long get_origin_node_id() {
        return origin_node;
    }

    @Override
    public Long get_destination_node_id() {
        return destination_node;
    }

}
