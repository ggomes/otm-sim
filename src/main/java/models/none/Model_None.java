package models.none;

import commodity.Commodity;
import commodity.Path;
import common.AbstractSource;
import common.Link;
import common.RoadConnection;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import geometry.FlowDirection;
import geometry.Side;
import jaxb.OutputRequest;
import models.AbstractLaneGroup;
import models.AbstractModel;
import output.AbstractOutput;
import output.animation.AbstractLinkInfo;
import profiles.DemandProfile;
import runner.Scenario;
import utils.StochasticProcess;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class Model_None extends AbstractModel {

    public Model_None(String name, boolean is_default, StochasticProcess process) {
        super(name, is_default, process);
    }

    @Override
    public void validate(OTMErrorLog errorLog) {

    }

    @Override
    public void reset(Link link) {

    }

    @Override
    public void build() {

    }

    @Override
    public AbstractOutput create_output_object(Scenario scenario, String prefix, String output_folder, OutputRequest jaxb_or) throws OTMException {
        return null;
    }

    @Override
    public AbstractLaneGroup create_lane_group(Link link, Side side, FlowDirection flowdir, Float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs) {
        return new models.none.LaneGroup(link,side,flowdir,length,num_lanes,start_lane,out_rcs);
    }

    @Override
    public AbstractSource create_source(Link origin, DemandProfile demand_profile, Commodity commodity, Path path) {
        return null;
    }

    @Override
    public AbstractLinkInfo get_link_info(Link link) {
        return null;
    }

    @Override
    public void register_with_dispatcher(Scenario scenario, Dispatcher dispatcher, float start_time) {

    }

    @Override
    public Map<AbstractLaneGroup, Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups) {
        return null;
    }
}
