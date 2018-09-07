/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package api.info;

import error.OTMException;
import keys.DemandType;
import profiles.AbstractDemandProfile;
import profiles.DemandProfile;

import java.util.ArrayList;
import java.util.List;

public class ODInfo {

    /** id of the origin node */
    public long origin_node_id;

    /** id of the destination node */
    public long destination_node_id;

    /** id of the commodity */
    public long commodity_id;

    /** total demand as a profile in vehicles per second. */
    public Profile1DInfo total_demand;

    /** list of subnetworks (i.e. routes) for this OD pair  */
    public List<SubnetworkInfo> subnetworks;

    public ODInfo(ODPair odpair){
        this.origin_node_id = odpair.origin_node_id;
        this.destination_node_id = odpair.destination_node_id;
        this.commodity_id = odpair.commodity_id;
        this.total_demand = null;
        this.subnetworks = new ArrayList<>();
    }

    public void add_demand_profile(AbstractDemandProfile demand_profile) throws OTMException {

        if(demand_profile.get_type()!=DemandType.pathfull)
            throw new OTMException("demand profile must be pathfull");

        if(demand_profile.get_origin_node_id()!= origin_node_id)
            throw new OTMException("demand_profile.path.get_origin_node_id()!=origin_node_id");

        if(demand_profile.get_destination_node_id()!= destination_node_id)
            throw new OTMException("demand_profile.path.get_destination_node_id()!=destination_node_id");

        if(demand_profile.commodity.getId()!=commodity_id)
            throw new OTMException("demand_profile.commodity.getId()!=commodity_id");

        if(total_demand ==null)
            total_demand = new Profile1DInfo(demand_profile.profile);
        else
            total_demand.add_profile(demand_profile.profile);

        if(demand_profile instanceof DemandProfile)
            this.subnetworks.add(new SubnetworkInfo(((DemandProfile) demand_profile).path));

    }

    public long get_origin_node_id() {
        return origin_node_id;
    }

    public long get_destination_node_id() {
        return destination_node_id;
    }

    public long get_commodity_id() {
        return commodity_id;
    }

    public Profile1DInfo get_total_demand_vps() {
        return total_demand;
    }

    public List<SubnetworkInfo> get_subnetworks() {
        return subnetworks;
    }

    @Override
    public String toString() {
        return origin_node_id+"\t"+destination_node_id+"\t"+commodity_id+"\t"+total_demand.getValues();
    }
}
