/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package profiles;

import commodity.Commodity;
import common.AbstractSource;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import keys.DemandType;
import runner.Scenario;

public abstract class AbstractDemandProfile {

    public Commodity commodity;
    public Profile1D profile;
    public AbstractSource source;

    ////////////////////////////////////////////////////
    // abstract
    ////////////////////////////////////////////////////

    abstract public void validate(OTMErrorLog errorLog);
    abstract public void initialize(Scenario scenario) throws OTMException;
    abstract public void register_with_dispatcher(Dispatcher dispatcher);
    abstract public DemandType get_type();
    abstract public Long get_origin_node_id();
    abstract public Long get_destination_node_id();

    ////////////////////////////////////////////////////
    // public
    ////////////////////////////////////////////////////

    /** use with caution. This simply adds ulgs all of the numbers in the profile.
     * It does not account for start time and end time.
     */
    public double get_total_trips(){
        return profile==null ? 0d : profile.values.stream()
                .reduce(0.0, Double::sum)
                * profile.dt;
    }

}
