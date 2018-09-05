package output;

import common.FlowAccumulatorSet;
import common.Link;
import error.OTMException;
import profiles.Profile1D;
import runner.Scenario;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubnetworkFlow extends AbstractOutputTimedSubnetwork {

    // map from link id to flow profile
    public Map<Long, Profile1D> flow;

    private Map<Long, FlowAccumulatorSet> flow_accumulator_sets;

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////


    public SubnetworkFlow(Scenario scenario, String prefix, String output_folder, Long commodity_id, Long subnetwork_id, Float outDt) throws OTMException {
        super(scenario, prefix, output_folder, commodity_id, subnetwork_id, outDt);
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
        flow_accumulator_sets = new HashMap<>();
        for(Link link : subnetwork.get_links_collection())
            flow_accumulator_sets.put(link.getId(),link.request_flow_accumulator_set(commodity==null ? null : commodity.getId()));

        if(!write_to_file) {
            flow = new HashMap<>();
            for(Link link : subnetwork.get_links_collection())
                flow.put(link.getId(), new Profile1D(null, outDt));
        }
    }

    @Override
    public String get_output_file() {
        return super.get_output_file() + "_subnetwork_flw.txt";
    }

    public Profile1D get_profile_for_linkid(Long link_id){
        return flow.get(link_id);
    }

    public List<Double> get_flow_for_link_in_vph(Long link_id){
        if(!flow.containsKey(link_id))
            return null;
        Profile1D profile = flow.get(link_id);
        profile.multiply(3600d/outDt);
        return profile.get_values();
    }

    public double get_flow_vph_for_linkid_timestep(Long link_id,int timestep) throws OTMException {
        Profile1D profile = get_profile_for_linkid(link_id);
        if(profile==null)
            throw new OTMException("Bad link id in get_flow_vph_for_linkid_timestep()");
        if(timestep<0 || timestep>=profile.values.size())
            throw new OTMException("Bad timestep in get_flow_vph_for_linkid_timestep()");
        return 3600*(profile.get_ith_value(timestep+1)-profile.get_ith_value(timestep))/profile.dt;
    }

    //////////////////////////////////////////////////////
    // write
    //////////////////////////////////////////////////////

    @Override
    public void write(float timestamp,Object obj) throws OTMException {

        if(write_to_file){
            super.write(timestamp,null);
            try {
                boolean isfirst=true;
                for(FlowAccumulatorSet fas : flow_accumulator_sets.values()){
                    if(!isfirst)
                        writer.write(AbstractOutputTimed.delim);
                    isfirst = false;
                    writer.write(String.format("%f",fas.get_vehicle_count()));
                    fas.reset();
                }
                writer.write("\n");
            } catch (IOException e) {
                throw new OTMException(e);
            }
        } else {
            for(Link link : subnetwork.get_links_collection()){
                FlowAccumulatorSet fas = flow_accumulator_sets.get(link.getId());
                flow.get(link.getId()).add(fas.get_vehicle_count());
                fas.reset();
            }
        }

    }

}
