/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package output;

import error.OTMException;
import common.Link;
import runner.Scenario;

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

    //////////////////////////////////////////////////////
    // implementation
    //////////////////////////////////////////////////////

    @Override
    public String get_output_file() {
        return super.get_output_file() + "_link_vht.txt";
    }

    @Override
    public String get_yaxis_label() {
        return "VHT [veh.hr]";
    }

    @Override
    public double get_value_for_link(Long link_id) {
        Link link = linkprofiles.get(link_id).link;
        return outDt_hr * link.get_veh_for_commodity(commodity.getId());
    }

}








