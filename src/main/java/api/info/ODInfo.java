package api.info;

import commodity.Path;
import commodity.Subnetwork;
import error.OTMException;
import keys.DemandType;
import profiles.AbstractDemandProfile;
import profiles.DemandProfile;
import runner.Scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public ODInfo(ODPair odpair, Scenario scenario){
        this.origin_node_id = odpair.origin_node_id;
        this.destination_node_id = odpair.destination_node_id;
        this.commodity_id = odpair.commodity_id;
        this.total_demand = null;
        this.subnetworks = new ArrayList<>();

        for(Subnetwork subnetwork : scenario.subnetworks.values()){
            if(subnetwork.is_path){
                Path path = (Path) subnetwork;
                if(path.ordered_links.get(0).start_node.getId()==origin_node_id &&
                   path.ordered_links.get(path.ordered_links.size()-1).end_node.getId()==destination_node_id )
                    subnetworks.add(new SubnetworkInfo(subnetwork));
            }
        }

    }

    public void add_demand_profile(AbstractDemandProfile demand_profile) throws OTMException {

        if(demand_profile.get_type()!=DemandType.pathfull)
            throw new OTMException("demand profile must be pathfull");

        if(!demand_profile.get_origin_node_id().equals(origin_node_id))
            throw new OTMException("demand_profile.path.get_origin_node_id()!=origin_node_id");

        if(demand_profile.get_destination_node_id()!= destination_node_id)
            throw new OTMException("demand_profile.path.get_destination_node_id()!=destination_node_id");

        if(demand_profile.commodity.getId()!=commodity_id)
            throw new OTMException("demand_profile.commodity.getId()!=commodity_id");

        if(total_demand ==null)
            total_demand = new Profile1DInfo(demand_profile.profile);
        else
            total_demand.add_profile(demand_profile.profile);

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
