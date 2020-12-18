package output;

import commodity.Path;
import commodity.Subnetwork;
import core.Link;
import error.OTMException;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import profiles.Profile1D;
import core.Scenario;

import java.io.*;

public abstract class AbstractOutputTimedPath extends AbstractOutputTimed {

    public Path path;
    public Profile1D profile;

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public AbstractOutputTimedPath(Scenario scenario, String prefix, String output_folder, Long commodity_id, Long subnetwork_id, Float outDt) throws OTMException {
        super(scenario,prefix,output_folder,commodity_id,outDt);

        // get subnetwork
        if(subnetwork_id==null) {
            path = null;
            return;
        }

        if (!scenario.subnetworks.containsKey(subnetwork_id))
            throw new OTMException("Bad subnetwork id in output request.");

        Subnetwork subnet = scenario.subnetworks.get(subnetwork_id);

        if(!(subnet instanceof Path))
            throw new OTMException("Subnetwork in output request is not a path.");

        path = (Path) subnet;
    }

    //////////////////////////////////////////////////////
    // InterfacePlottable
    //////////////////////////////////////////////////////

    @Override
    public void plot(String filename) throws OTMException {
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(get_series());
        make_time_chart(dataset,String.format("Subnetwork %d",path.getId()),get_yaxis_label(),filename);
    }

    //////////////////////////////////////////////////////
    // AbstractOutput
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
                    for (Link link : path.get_ordered_links() )
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

    //////////////////////////////////////////////////////
    // final
    //////////////////////////////////////////////////////

    public final Long get_path_id(){
        return path.getId();
    }

    public final XYSeries get_series() {
        return profile.get_series(String.format("%d", get_path_id()));
    }

}
