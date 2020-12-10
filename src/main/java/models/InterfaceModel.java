package models;

import commodity.Commodity;
import commodity.Path;
import core.*;
import core.geometry.Side;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.OutputRequest;
import output.AbstractOutput;
import output.animation.AbstractLinkInfo;
import profiles.Profile1D;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface InterfaceModel {


    void reset(Link link);
    void validate(OTMErrorLog errorLog);
    void register_with_dispatcher(Scenario scenario, Dispatcher dispatcher, float start_time);

    // building
    AbstractOutput create_output_object(Scenario scenario, String prefix, String output_folder, OutputRequest jaxb_or)  throws OTMException;
    AbstractLaneGroup create_lane_group(Link link, Side side, Float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs, jaxb.Roadparam rp);
    AbstractDemandGenerator create_source(Link origin, Profile1D profile, Commodity commodity, Path path);

    // execution
    Map<AbstractLaneGroup,Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups);

    // output
    AbstractLinkInfo get_link_info(Link link);

}
