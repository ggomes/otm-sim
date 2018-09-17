package output;

import common.Link;
import error.OTMException;
import org.jfree.data.xy.XYSeries;
import profiles.Profile1D;
import runner.Scenario;

import java.io.IOException;
import java.util.List;

public class LinkVehiclesGlobal extends AbstractOutputTimedLink {

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public LinkVehiclesGlobal(Scenario scenario, String prefix, String output_folder, List<Long> link_ids, Float outDt) throws OTMException {
        super(scenario,prefix,output_folder,null,link_ids,outDt);
        this.type = Type.link_veh;
    }

    @Override
    public String get_output_file() {
        return super.get_output_file() + "_link_veh.txt";
    }

    @Override
    String get_yaxis_label() {
        return "vehicles [veh]";
    }

    @Override
    XYSeries get_series_for_linkid(Long link_id) {
        return values.get(link_id).get_series(String.format("%d",link_id));
    }

    //////////////////////////////////////////////////////
    // get
    //////////////////////////////////////////////////////

    public List<Double> get_density_for_link_in_vpk(Long link_id){
        if(!links.containsKey(link_id))
            return null;
        Profile1D profile = values.get(link_id).clone();
        profile.multiply(1000d/links.get(link_id).length);
        return profile.get_values();
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
                for(Link link : links.values()){
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
            for (Link link : links.values())
                values.get(link.getId()).add(get_current_value_for_link(link));
        }
    }

    //////////////////////////////////////////////////////
    // private
    //////////////////////////////////////////////////////

    private double get_current_value_for_link(Link link){
        return link.get_veh_for_commodity(null);
    }

}
