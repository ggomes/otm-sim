/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
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

    public Map<Long,Link> links;
    public Map<Long, Profile1D> values;

    abstract String get_yaxis_label();
    abstract XYSeries get_series_for_linkid(Long link_id);

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public AbstractOutputTimedLink(Scenario scenario,Float outDt) throws OTMException
    {
        super(scenario,null,null,null,outDt);

        links = scenario.network.links;
    }

    public AbstractOutputTimedLink(Scenario scenario,String prefix,String output_folder,Long commodity_id,List<Long> link_ids,Float outDt) throws OTMException
    {
        super(scenario,prefix,output_folder,commodity_id,outDt);

        if(link_ids==null)
            link_ids = scenario.network.links.values().stream().map(link->link.getId()).collect(Collectors.toList());

        links = new HashMap<>();
        for(Long link_id : link_ids){
            Link link = scenario.network.links.get(link_id);
            if(link!=null)
                links.put(link_id,link);
        }
    }

    public AbstractOutputTimedLink(Scenario scenario,String prefix,String output_folder,Long commodity_id,Long subnetwork_id,Float outDt) throws OTMException
    {
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
        if(subnetwork==null)
            links = scenario.network.links;
        else {
            links = new HashMap<>();
            for(Link link : subnetwork.links)
                links.put(link.getId(),link);
        }

    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);

        if(links.isEmpty())
            errorLog.addError("no links in output request");
    }

    //////////////////////////////////////////////////////
    // get / plot
    //////////////////////////////////////////////////////

    public Collection<Long> get_link_ids(){
        return links.keySet();
    }

    public Profile1D get_profile_for_linkid(Long link_id){
        return values.get(link_id);
    }

    public void plot_for_links(Collection<Long> link_ids,String filename) throws OTMException {

        if(link_ids==null)
            link_ids = links.keySet();

        XYSeriesCollection dataset = new XYSeriesCollection();

        for(Long link_id : link_ids) {
            if(!values.containsKey(link_id))
                throw new OTMException("Bad link id " + link_id);
            dataset.addSeries(get_series_for_linkid(link_id));
        }

        make_time_chart(dataset,get_yaxis_label(),filename);
    }

    //////////////////////////////////////////////////////
    // write links
    //////////////////////////////////////////////////////

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
                    for (Link link : links.values())
                        links_writer.write(link.getId() + "\t");
                    links_writer.close();
                }
            } catch (FileNotFoundException exc) {
                throw new OTMException(exc);
            } catch (IOException e) {
                throw new OTMException(e);
            }
        }
        else {
            values = new HashMap<>();
            for(Link link : links.values())
                values.put(link.getId(), new Profile1D(null, outDt));
        }
    }

}
