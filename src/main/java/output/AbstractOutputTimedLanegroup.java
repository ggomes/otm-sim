package output;

import common.AbstractLaneGroup;
import common.Link;
import error.OTMErrorLog;
import error.OTMException;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import profiles.Profile1D;
import common.Scenario;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractOutputTimedLanegroup extends AbstractOutputTimed {

    public ArrayList<AbstractLaneGroup> ordered_lgs;
    public Map<Long, LaneGroupProfile> lgprofiles;
    abstract protected double get_value_for_lanegroup(AbstractLaneGroup lg);

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public AbstractOutputTimedLanegroup(Scenario scenario,String prefix,String output_folder,Long commodity_id,Collection<Long> link_ids,Float outDt) throws OTMException {
        super(scenario,prefix,output_folder,commodity_id,outDt);

        // get lanegroup list
        if(link_ids==null)
            link_ids = new ArrayList<>(scenario.network.links.keySet());

        ordered_lgs = new ArrayList<>();
        lgprofiles = new HashMap<>();
        for(Long link_id : link_ids){
            if(!scenario.network.links.containsKey(link_id))
                continue;
            Link link = scenario.network.links.get(link_id);
            for(AbstractLaneGroup lg : link.lanegroups_flwdn.values()){
                ordered_lgs.add(lg);
                lgprofiles.put(lg.id, new LaneGroupProfile(lg));
            }
        }

    }

    //////////////////////////////////////////////////////
    // AbstractOutputTimed
    //////////////////////////////////////////////////////

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
                LaneGroupProfile lgProfile = lgprofiles.get(lg.id);
                lgProfile.add_value(get_value_for_lanegroup(lg));
            }
        }
    }

    //////////////////////////////////////////////////////
    // AbstractOutput
    //////////////////////////////////////////////////////

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);

        if(lgprofiles.isEmpty())
            errorLog.addError("no lanegroups in output request");
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);

        if(write_to_file){
            try {
                String filename = get_output_file();
                if(filename!=null) {
                    String subfilename = filename.substring(0,filename.length()-4);
                    Writer lanegroups_writer = new OutputStreamWriter(new FileOutputStream(subfilename + "_lanegroups.txt"));
                    for(LaneGroupProfile lgprof: lgprofiles.values()){
                        AbstractLaneGroup lg = lgprof.lg;
                        lanegroups_writer.write(lg.id+" "+lg.link.getId() + " " + lg.start_lane_dn+ " " + (lg.start_lane_dn+lg.num_lanes-1) +"\n"); // start/end dn lanes
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

    //////////////////////////////////////////////////////
    // incomplete implementation
    //////////////////////////////////////////////////////

    public XYSeries get_series_for_lg(AbstractLaneGroup lg) {
        if(!lgprofiles.containsKey(lg.id))
            return null;
        return lgprofiles.get(lg.id).profile.get_series(String.format("%d (%d-%d)",lg.link.getId(),lg.start_lane_dn,lg.start_lane_dn+lg.num_lanes-1));
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
        for(AbstractLaneGroup lg : scenario.network.links.get(link_id).lanegroups_flwdn.values())
            if(lgprofiles.containsKey(lg.id))
                profiles.put(lg.id,lgprofiles.get(lg.id).profile);

        return profiles;
    }

    public final void plot_for_links(Set<Long> link_ids,String filename) throws OTMException {

        Set<AbstractLaneGroup> lgs = new HashSet<>();
        if(link_ids==null)
            lgs.addAll(ordered_lgs);
        else
            lgs = ordered_lgs.stream().filter(lg->link_ids.contains(lg.link.getId())).collect(Collectors.toSet());

        XYSeriesCollection dataset = new XYSeriesCollection();

        for(AbstractLaneGroup lg : lgs)
            dataset.addSeries(get_series_for_lg(lg));

        make_time_chart(dataset,get_yaxis_label(),filename);
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
            profile.add(value);
        }
    }
}
