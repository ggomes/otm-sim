package output;

import common.AbstractLaneGroup;
import common.Link;
import error.OTMException;
import org.jfree.data.xy.XYSeries;
import profiles.Profile1D;
import runner.Scenario;
import sensor.AccumulatorGlobal;

import java.io.IOException;
import java.util.*;

public class LinkCountGlobal extends AbstractOutputTimedLink {

    private Map<Long, Set<AccumulatorGlobal>> flw_acc_sets; // link_id -> flow accumulator set (over lgs)

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public LinkCountGlobal(Scenario scenario, String prefix, String output_folder, List<Long> link_ids, Float outDt) throws OTMException {
        super(scenario,prefix,output_folder,null,link_ids,outDt);
        this.type = Type.link_flw;
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
        flw_acc_sets = new HashMap<>();
        for(Link link : links.values()) {
            Set<AccumulatorGlobal> flw_acc_set = new HashSet<>();
            flw_acc_sets.put(link.getId(),flw_acc_set);
            for(AbstractLaneGroup lg : link.lanegroups.values())
                flw_acc_set.add(lg.request_flow_accumulator());
        }
    }

    @Override
    public String get_output_file() {
        return super.get_output_file() + "_link_flw_global.txt";
    }

    @Override
    String get_yaxis_label() {
        return "Flow [veh/hr]";
    }

    @Override
    XYSeries get_series_for_linkid(Long link_id) {
        Profile1D profile = get_flow_profile_for_link_in_vph(link_id);
        if(profile==null)
            return null;
        else
            return profile.get_series(String.format("%d",link_id));
    }

    //////////////////////////////////////////////////////
    // get
    //////////////////////////////////////////////////////

    public Profile1D get_flow_profile_for_link_in_vph(Long link_id){
        if(!values.containsKey(link_id))
            return null;
        Profile1D profile = values.get(link_id).clone();
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

    //////////////////////////////////////////////////////
    // write
    //////////////////////////////////////////////////////

    @Override
    public void write(float timestamp,Object obj) throws OTMException {

        if(write_to_file){
            super.write(timestamp,null);
            try {
                boolean isfirst=true;
                for(Set<AccumulatorGlobal> fas : flw_acc_sets.values()){
                    if(!isfirst)
                        writer.write(AbstractOutputTimed.delim);
                    isfirst = false;
                    writer.write(String.format("%f",fas.stream().mapToDouble(x->x.count).sum()));
                }
                writer.write("\n");
            } catch (IOException e) {
                throw new OTMException(e);
            }
        } else {
            for(Long link_id : links.keySet()){
                Set<AccumulatorGlobal> fas = flw_acc_sets.get(link_id);
                values.get(link_id).add(fas.stream().mapToDouble(x->x.count).sum());
           }
        }

    }

}
