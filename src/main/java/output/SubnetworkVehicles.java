/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package output;

import common.Link;
import error.OTMException;
import profiles.Profile1D;
import runner.Scenario;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubnetworkVehicles extends AbstractOutputTimedSubnetwork {

    // map from link id to profile
    public Map<Long,Profile1D> vehicles;

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public SubnetworkVehicles(Scenario scenario, String prefix, String output_folder, Long commodity_id, Long subnetwork_id, Float outDt) throws OTMException {
        super(scenario, prefix, output_folder, commodity_id, subnetwork_id, outDt);
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);

        if(!write_to_file) {
            vehicles = new HashMap<>();
            for(Link link : subnetwork.links)
                vehicles.put(link.getId(),new Profile1D(null, outDt));
        }
    }

    @Override
    public String get_output_file() {
        return super.get_output_file() + "_subnetwork_veh.txt";
    }

    public Profile1D get_profile_for_linkid(Long link_id){
        return vehicles.get(link_id);
    }

    public List<Double> get_density_for_link_in_vpk(Long link_id){
        if(!vehicles.containsKey(link_id))
            return null;
        Profile1D profile = vehicles.get(link_id);
        Link link = scenario.network.links.get(link_id);
        profile.multiply(1000d/link.length);
        return profile.get_values();
    }

    //////////////////////////////////////////////////////
    // write
    //////////////////////////////////////////////////////

    private double get_current_value_for_link(Link link){
        return link.get_veh_for_commodity(commodity==null?null:commodity.getId());
    }

    @Override
    public void write(float timestamp,Object obj) throws OTMException {

        if(write_to_file){
            super.write(timestamp,null);
            try {
                boolean isfirst=true;
                for(Link link : subnetwork.get_links_collection()){
                    if(!isfirst)
                        writer.write(AbstractOutputTimed.delim);
                    isfirst = false;
                    writer.write(String.format("%f", get_current_value_for_link(link)));
                }
                writer.write("\n");
            } catch (IOException e) {
                throw new OTMException(e);
            }
        } else {
            for (Link link : subnetwork.get_links_collection())
                vehicles.get(link.getId()).add(get_current_value_for_link(link));
        }
    }

}
