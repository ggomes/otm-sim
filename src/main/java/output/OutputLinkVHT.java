package output;

import error.OTMException;
import common.Link;
import common.Scenario;

public class OutputLinkVHT extends AbstractOutputTimedLink {

    public double outDt_hr;

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public OutputLinkVHT(Scenario scenario, String prefix, String output_folder, Long commodity_id, Long subnetwork_id, Float outDt) throws OTMException {
        super(scenario,prefix,output_folder,commodity_id,subnetwork_id,outDt);
        this.type = Type.vht;
        outDt_hr = this.outDt / 3600.0;
    }

    //////////////////////////////////////////////////////
    // InterfaceOutput
    //////////////////////////////////////////////////////

    @Override
    public String get_output_file() {
        return write_to_file ? super.get_output_file() + "_sumveh.txt" : null;
    }

    //////////////////////////////////////////////////////
    // InterfacePlottable
    //////////////////////////////////////////////////////

    @Override
    public String get_yaxis_label() {
        return "VHT [veh.hr]";
    }

    //////////////////////////////////////////////////////
    // AbstractOutputTimedLink
    //////////////////////////////////////////////////////

    @Override
    public double get_value_for_link(Long link_id) {
        Link link = linkprofiles.get(link_id).link;
        return outDt_hr * link.get_veh_for_commodity(commodity.getId());
    }

}








