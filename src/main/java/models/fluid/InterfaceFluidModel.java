package models.fluid;

import common.Link;
import error.OTMException;
import models.InterfaceModel;

public interface InterfaceFluidModel extends InterfaceModel {
    void compute_lanechange_demand_supply(Link link, float timestamp) throws OTMException;
    void update_link_state(Link link,float timestamp) throws OTMException;
    AbstractCell create_cell(float cell_length_meters, FluidLaneGroup lg) throws OTMException;
}
