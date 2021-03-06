package core;

import commodity.Commodity;
import commodity.Path;
import core.geometry.Side;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.OutputRequest;
import output.AbstractOutput;
import profiles.Profile1D;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface InterfaceModel {

    // object factory
    AbstractOutput create_output(Scenario scenario, String prefix, String output_folder, OutputRequest jaxb_or)  throws OTMException;
    AbstractLaneGroup create_lane_group(Link link, Side side, Float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs, jaxb.Roadparam rp) throws OTMException ;
    AbstractDemandGenerator create_source(Link origin, Profile1D profile, Commodity commodity, Path path);

    // initialization
    void set_state_for_link(Link link);
    void validate_pre_init(OTMErrorLog errorLog);
    void validate_post_init(OTMErrorLog errorLog);
    void register_with_dispatcher(Scenario scenario, Dispatcher dispatcher, float start_time);

    //
    Map<AbstractLaneGroup,Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups);

}
