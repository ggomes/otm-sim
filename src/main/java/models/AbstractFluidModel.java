package models;

import common.Link;

public abstract class AbstractFluidModel extends AbstractModel {

    public AbstractFluidModel(String name, boolean is_default) {
        super(name, is_default);
    }

    //////////////////////////////////////////////////////////////
    // load
    //////////////////////////////////////////////////////////////

    @Override
    public void set_road_param(Link link, jaxb.Roadparam r) {
        super.set_road_param(link,r);
        // send parameters to lane groups
        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values())
            lg.set_road_params(r);
    }

}
