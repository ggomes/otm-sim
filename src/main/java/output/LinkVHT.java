package output;

import error.OTMException;
import common.Link;
import org.jfree.data.xy.XYSeries;
import profiles.Profile1D;
import runner.Scenario;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LinkVHT extends AbstractOutputTimedLink {

    public double outDt_hr;

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public LinkVHT(Scenario scenario,String prefix,String output_folder,Long commodity_id,Long subnetwork_id,Float outDt) throws OTMException {
        super(scenario,prefix,output_folder,commodity_id,subnetwork_id,outDt);
        this.type = Type.vht;
        outDt_hr = this.outDt / 3600.0;
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
    }

    @Override
    public String get_output_file() {
        return super.get_output_file() + "_link_vht.txt";
    }

    @Override
    String get_yaxis_label() {
        return "VHT [veh.hr]";
    }


    @Override
    XYSeries get_series_for_linkid(Long link_id) {
        return values.get(link_id).get_series(String.format("%d",link_id));
    }

    //////////////////////////////////////////////////////
    // get
    //////////////////////////////////////////////////////

    public double get_value_for_link(Link link){
        return outDt_hr * link.get_veh_for_commodity(commodity.getId());
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
                    writer.write(String.format("%f",get_value_for_link(link)));
                }
                writer.write("\n");
            } catch (IOException e) {
                throw new OTMException(e);
            }
        } else {
            for (Link link : links.values())
                values.get(link.getId()).add(get_value_for_link(link));
        }
    }

}








