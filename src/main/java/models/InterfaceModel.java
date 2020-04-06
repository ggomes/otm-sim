package models;

import commodity.Commodity;
import commodity.Path;
import common.AbstractSource;
import common.Link;
import common.RoadConnection;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import geometry.FlowPosition;
import geometry.Side;
import jaxb.OutputRequest;
import output.AbstractOutput;
import output.animation.AbstractLinkInfo;
import profiles.DemandProfile;
import common.Scenario;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface InterfaceModel {

    void reset(Link link);
    void validate(OTMErrorLog errorLog);
    void register_with_dispatcher(Scenario scenario, Dispatcher dispatcher, float start_time);

    AbstractOutput create_output_object(Scenario scenario, String prefix, String output_folder, OutputRequest jaxb_or)  throws OTMException;
    AbstractLaneGroup create_lane_group(Link link, Side side, FlowPosition flwpos, Float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs);
    AbstractSource create_source(Link origin, DemandProfile demand_profile, Commodity commodity, Path path);

    Map<AbstractLaneGroup,Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups);
    AbstractLinkInfo get_link_info(Link link);

}
