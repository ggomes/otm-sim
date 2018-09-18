package output;

import error.OTMException;
import org.jfree.data.xy.XYSeries;
import runner.Scenario;

import java.util.List;

public class LinkCountCommodityPath extends AbstractOutputTimedLink  {

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public LinkCountCommodityPath(Scenario scenario, String prefix, String output_folder, Long commodity_id, List<Long> link_ids, Float outDt) throws OTMException {
        super(scenario,prefix,output_folder,commodity_id,link_ids,outDt);
        this.type = Type.link_flw;

        throw new OTMException("LinkCountCommodityPath not implemented.");
    }

    @Override
    String get_yaxis_label() {
        return null;
    }

    @Override
    XYSeries get_series_for_linkid(Long link_id) {
        return null;
    }
}
