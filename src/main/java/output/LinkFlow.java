package output;

import common.FlowAccumulatorState;
import models.BaseLaneGroup;
import error.OTMException;
import profiles.Profile1D;
import runner.Scenario;

import java.util.*;

public class LinkFlow extends AbstractOutputTimedLink {

    private Map<Long, Set<FlowAccumulatorState>> flw_acc_sets; // link_id -> flow accumulator set (over lgs)

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public LinkFlow(Scenario scenario, String prefix, String output_folder, Long commodity_id, Collection<Long> link_ids, Float outDt) throws OTMException {
        super(scenario,prefix,output_folder,commodity_id,link_ids,outDt);
        this.type = Type.link_flw;
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
        flw_acc_sets = new HashMap<>();
        for(LinkProfile linkProfile : linkprofiles.values()) {
            Set<FlowAccumulatorState> flw_acc_set = new HashSet<>();
            flw_acc_sets.put(linkProfile.link.getId(),flw_acc_set);
            for(BaseLaneGroup lg : linkProfile.link.lanegroups_flwdn.values())
                flw_acc_set.add(lg.request_flow_accumulator(commodity==null ? null : commodity.getId()));
        }
    }

    //////////////////////////////////////////////////////
    // implementation
    //////////////////////////////////////////////////////

    @Override
    public String get_output_file() {
        return write_to_file ? super.get_output_file() + "_link_flw_global.txt" : null;
    }

    @Override
    public String get_yaxis_label() {
        return "Flow [veh/hr]";
    }

    @Override
    public double get_value_for_link(Long link_id) {
        Set<FlowAccumulatorState> fas = flw_acc_sets.get(link_id);
        return fas.stream().mapToDouble(x->x.get_total_count()).sum();
    }

    //////////////////////////////////////////////////////
    // get
    //////////////////////////////////////////////////////

    public Profile1D get_flow_profile_for_link_in_vph(Long link_id){
        if(!linkprofiles.containsKey(link_id))
            return null;
        Profile1D profile = linkprofiles.get(link_id).profile.clone();
        Profile1D diffprofile = new Profile1D(profile.start_time,profile.dt,profile.diff());
        diffprofile.multiply(3600d/outDt);
        return diffprofile;
    }

    public List<Double> get_flow_for_link_in_vph(Long link_id){
        Profile1D profile = get_flow_profile_for_link_in_vph(link_id);
        if(profile==null)
            return null;
        else
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

}
