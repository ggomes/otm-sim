package output;

import commodity.Subnetwork;
import common.Link;
import error.OTMErrorLog;
import error.OTMException;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import profiles.Profile1D;
import common.Scenario;

import java.io.*;

public abstract class AbstractOutputTimedSubnetwork extends AbstractOutputTimed {

    public Subnetwork subnetwork;
    public Profile1D profile;

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public AbstractOutputTimedSubnetwork(Scenario scenario, String prefix, String output_folder, Long commodity_id, Long subnetwork_id, Float outDt) throws OTMException {
        super(scenario,prefix,output_folder,commodity_id,outDt);

        // get subnetwork
        if(subnetwork_id==null)
            subnetwork = null;
        else{
            if (!scenario.subnetworks.containsKey(subnetwork_id))
                throw new OTMException("Bad subnetwork id in output request.");

            subnetwork = scenario.subnetworks.get(subnetwork_id);
        }
    }

    public Long get_subnetwork_id(){
        return subnetwork==null ? null : subnetwork.getId();
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);
        if (subnetwork == null)
            errorLog.addError("Bad path id in AbstractOutputTimedSubnetwork");
        else
            subnetwork.validate(errorLog);
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
                    for (Link link : subnetwork.get_links_collection())
                        links_writer.write(link.getId() + "\t");
                    links_writer.close();
                }
            } catch (FileNotFoundException exc) {
                throw new OTMException(exc);
            } catch (IOException e) {
                throw new OTMException(e);
            }
        }
    }

    /////////////////////////
    // plotting
    /////////////////////////

    public XYSeries get_series() {
        return profile.get_series(String.format("%d",get_subnetwork_id()));
    }

    public void plot(String filename) throws OTMException {
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(get_series());
        make_time_chart(dataset,get_yaxis_label(),filename);
    }

}
