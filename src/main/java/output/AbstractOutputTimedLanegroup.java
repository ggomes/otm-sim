package output;

import core.AbstractLaneGroup;
import core.Link;
import error.OTMErrorLog;
import error.OTMException;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import profiles.Profile1D;
import core.Scenario;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractOutputTimedLanegroup extends AbstractOutputTimed {

    public Collection<Long> link_ids;
    public ArrayList<AbstractLaneGroup> ordered_lgs;
    public Map<Long, LaneGroupProfile> lgprofiles;
    abstract protected double get_value_for_lanegroup(AbstractLaneGroup lg);

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public AbstractOutputTimedLanegroup(Scenario scenario,String prefix,String output_folder,Long commodity_id,Collection<Long> inlink_ids,Float outDt) throws OTMException {
        super(scenario,prefix,output_folder,commodity_id,outDt);

        // get lanegroup list
        if(inlink_ids==null)
            this.link_ids = new ArrayList<>(scenario.network.links.keySet());
        else {

            if(inlink_ids.stream().anyMatch(linkid->!scenario.network.links.containsKey(linkid)))
                throw new OTMException("Bad link id in lanegroup output request.");

            this.link_ids = inlink_ids;
        }

    }

    //////////////////////////////////////////////////////
    // AbstractOutputTimed
    //////////////////////////////////////////////////////

    @Override
    public String get_output_file() {
        return super.get_output_file() + "_lg";
    }

    @Override
    public final void write(float timestamp) throws OTMException {
        super.write(timestamp);
        if(write_to_file){
            try {
                boolean isfirst=true;
                for(AbstractLaneGroup lg : ordered_lgs){
                    if(!isfirst)
                        writer.write(AbstractOutputTimed.delim);
                    isfirst = false;
                    writer.write(String.format("%f",get_value_for_lanegroup(lg)));
                }
                writer.write("\n");
            } catch (IOException e) {
                throw new OTMException(e);
            }
        } else {
            for(AbstractLaneGroup lg : ordered_lgs){
                LaneGroupProfile lgProfile = lgprofiles.get(lg.getId());
                lgProfile.add_value(get_value_for_lanegroup(lg));
            }
        }
    }

    //////////////////////////////////////////////////////
    // AbstractOutput
    //////////////////////////////////////////////////////

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);

        ordered_lgs = new ArrayList<>();
        lgprofiles = new HashMap<>();
        for(Long link_id : link_ids){
            Link link = scenario.network.links.get(link_id);
            for(AbstractLaneGroup lg : link.get_lgs()){
                ordered_lgs.add(lg);
                lgprofiles.put(lg.getId(), new LaneGroupProfile(lg));
            }
        }

        if(write_to_file){
            try {
                String filename = get_output_file();
                if(filename!=null) {
                    String subfilename = filename.substring(0,filename.length()-4);
                    Writer lanegroups_writer = new OutputStreamWriter(new FileOutputStream(subfilename + "_cols.txt"));
                    for(LaneGroupProfile lgprof: lgprofiles.values()){
                        AbstractLaneGroup lg = lgprof.lg;
                        lanegroups_writer.write(lg.getId()+","+lg.get_link().getId() + "," + lg.get_start_lane_dn()+ "," + (lg.get_start_lane_dn()+lg.get_num_lanes()-1) +"\n"); // start/end dn lanes
                    }
                    lanegroups_writer.close();
                }
            } catch (FileNotFoundException exc) {
                throw new OTMException(exc);
            } catch (IOException e) {
                throw new OTMException(e);
            }
        } else {
            for(LaneGroupProfile lgProfile : lgprofiles.values())
                lgProfile.initialize(outDt);
        }

    }


    @Override
    public void validate_post_init(OTMErrorLog errorLog) {
        super.validate_post_init(errorLog);

        if(lgprofiles.isEmpty())
            errorLog.addError("no lanegroups in output request");
    }

    //////////////////////////////////////////////////////
    // incomplete implementation
    //////////////////////////////////////////////////////

    public XYSeries get_series_for_lg(AbstractLaneGroup lg) {
        if(!lgprofiles.containsKey(lg.getId()))
            return null;
        return lgprofiles.get(lg.getId()).profile.get_series(String.format("%d (%d-%d)",lg.get_link().getId(),lg.get_start_lane_dn(),lg.get_start_lane_dn()+lg.get_num_lanes()-1));
    }

    //////////////////////////////////////////////////////
    // final
    //////////////////////////////////////////////////////

    public final Map<Long,Profile1D> get_profiles_for_linkid(Long link_id){

        if(!scenario.network.links.containsKey(link_id))
            return null;
        if(write_to_file)
            return null;

        Map<Long,Profile1D> profiles = new HashMap<>();
        for(AbstractLaneGroup lg : scenario.network.links.get(link_id).get_lgs())
            if(lgprofiles.containsKey(lg.getId()))
                profiles.put(lg.getId(),lgprofiles.get(lg.getId()).profile);

        return profiles;
    }

    public final void plot_for_links(Set<Long> link_ids,String filename) throws OTMException {

        Set<AbstractLaneGroup> lgs = new HashSet<>();
        if(link_ids==null)
            lgs.addAll(ordered_lgs);
        else
            lgs = ordered_lgs.stream().filter(lg -> link_ids.contains(lg.get_link().getId())).collect(Collectors.toSet());

        XYSeriesCollection dataset = new XYSeriesCollection();

        for(AbstractLaneGroup lg : lgs)
            dataset.addSeries(get_series_for_lg(lg));

        String title = String.format("%s, comm: %s", type.name(), commodity==null ? "all" : commodity.name);
        make_time_chart(dataset,title,get_yaxis_label(),filename);
    }

    //////////////////////////////////////////////////////
    // class
    //////////////////////////////////////////////////////

    public class LaneGroupProfile {
        public AbstractLaneGroup lg;
        public Profile1D profile;
        public LaneGroupProfile(AbstractLaneGroup lg){
            this.lg = lg;
        }
        public void initialize(float outDt){
            this.profile = new Profile1D(null, outDt);
        }
        public void add_value(double value){
            profile.add_entry(value);
        }
    }
}
