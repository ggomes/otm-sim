package core;

import error.OTMException;
import models.fluid.AbstractCell;
import models.fluid.FluidLaneGroup;

public interface InterfaceFluidModel extends InterfaceModel {
    void compute_lanechange_demand_supply(Link link, float timestamp) throws OTMException;
    void update_link_state(Link link,float timestamp) throws OTMException;
    AbstractCell create_cell(FluidLaneGroup lg) throws OTMException;
}
