package output;

import commodity.Subnetwork;
import error.OTMErrorLog;
import error.OTMException;
import common.Link;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import profiles.Profile1D;
import runner.Scenario;

import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractOutputTimedLink extends AbstractOutputTimed {

    public List<Long> ordered_ids;
    public Map<Long,LinkProfile> linkprofiles;

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public AbstractOutputTimedLink(Scenario scenario,String prefix,String output_folder,Long commodity_id,Collection<Long> link_ids,Float outDt) throws OTMException {
        super(scenario,prefix,output_folder,commodity_id,outDt);

        if(link_ids==null)
            link_ids = scenario.network.links.values().stream().map(link->link.getId()).collect(Collectors.toSet());

        ordered_ids = new ArrayList<>();
        linkprofiles = new HashMap<>();
        for(Long link_id : link_ids){
            Link link = scenario.network.links.get(link_id);
            if(link!=null) {
                ordered_ids.add(link.getId());
                linkprofiles.put(link.getId(), new LinkProfile(link));
            }
        }
    }

    public AbstractOutputTimedLink(Scenario scenario,String prefix,String output_folder,Long commodity_id,Long subnetwork_id,Float outDt) throws OTMException {
        super(scenario,prefix,output_folder,commodity_id,outDt);

        // get subnetwork
        Subnetwork subnetwork;
        if(subnetwork_id==null)
            subnetwork = null;
        else{
            subnetwork = scenario.subnetworks.get(subnetwork_id);
            if(subnetwork==null)
                throw new OTMException("Bad subnetwork id in output request.");
        }

        // subnetwork==null, all links in common, otherwise, all links in subnetwork
        ordered_ids = new ArrayList<>();
        linkprofiles = new HashMap<>();
        for(Link link : subnetwork==null?scenario.network.links.values():subnetwork.links) {
            ordered_ids.add(link.getId());
            linkprofiles.put(link.getId(), new LinkProfile(link));
        }

    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);

        if(linkprofiles.isEmpty())
            errorLog.addError("no links in output request");
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
        // write links
        if(write_to_file) {
            try {
                String filename = get_output_file();
                if (filename != null) {
                    String subfilename = filename.substring(0, filename.length() - 4);
                    Writer links_writer = new OutputStreamWriter(new FileOutputStream(subfilename + "_links.txt"));
                    for (Long link_id : ordered_ids)
                        links_writer.write(link_id + "\t");
                    links_writer.close();
                }
            } catch (FileNotFoundException exc) {
                throw new OTMException(exc);
            } catch (IOException e) {
                throw new OTMException(e);
            }
        } else {
            for(LinkProfile linkProfile : linkprofiles.values())
                linkProfile.initialize(outDt);
        }
    }

    //////////////////////////////////////////////////////
    // get / plot
    //////////////////////////////////////////////////////

    public List<Float> get_time(){
        if(linkprofiles==null || linkprofiles.isEmpty())
            return new ArrayList();
        return linkprofiles.values().iterator().next().profile.get_times();
    }

    abstract public double get_value_for_link(Long link_id);

    public List<Long> get_link_ids(){
        return ordered_ids;
    }

    public Profile1D get_profile_for_linkid(Long link_id){
        return linkprofiles.get(link_id).profile;
    }

    public void plot_for_links(Set<Long> link_ids,String filename) throws OTMException {

        if(link_ids==null)
            link_ids = linkprofiles.keySet();

        XYSeriesCollection dataset = new XYSeriesCollection();

        for(Long link_id : link_ids) {
            if(!linkprofiles.containsKey(link_id))
                throw new OTMException("Bad link id " + link_id);
            dataset.addSeries(get_series_for_linkid(link_id));
        }

        make_time_chart(dataset,get_yaxis_label(),filename);
    }

    public XYSeries get_series_for_linkid(Long link_id) {
        if(!linkprofiles.containsKey(link_id))
            return null;
        return linkprofiles.get(link_id).profile.get_series(String.format("%d",link_id));
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
                for(Long link_id : ordered_ids){
                    if(!isfirst)
                        writer.write(AbstractOutputTimed.delim);
                    isfirst = false;
                    writer.write(String.format("%f",get_value_for_link(link_id)));
                }
                writer.write("\n");
            } catch (IOException e) {
                throw new OTMException(e);
            }
        } else {
            for(Long link_id : ordered_ids) {
                LinkProfile linkProfile = linkprofiles.get(link_id);
                linkProfile.add_value(get_value_for_link(link_id));
            }
        }
    }

    //////////////////////////////////////////////////////
    // class
    //////////////////////////////////////////////////////

    public class LinkProfile {
        public Link link;
        public Profile1D profile;
        public LinkProfile(Link link){
            this.link = link;
        }
        public void initialize(float outDt){
            this.profile = new Profile1D(null, outDt);
        }
        public void add_value(double value){
            profile.add(value);
        }
    }

}
