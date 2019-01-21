package models.micro;

import commodity.Commodity;
import commodity.Path;
import commodity.Subnetwork;
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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class Model_Micro extends AbstractModel {

    public float dt;

    public Model_Micro(String name, boolean is_default, Float dt) {
        super(name, is_default);
        this.dt = dt==null ? -1 : dt;
        myPacketClass = models.micro.PacketLaneGroup.class;
        System.out.println("MICRO constructor");
    }

    //////////////////////////
    // keep
    //////////////////////////

    @Override
    public Map<AbstractLaneGroup, Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups) {
        System.out.println("MICRO lanegroup_proportions");
        return null;
    }

    @Override
    public AbstractOutput create_output_object(Scenario scenario, String prefix, String output_folder, OutputRequest jaxb_or) throws OTMException {
        System.out.println("MICRO create_output_object");
        return null;
    }

    @Override
    public void register_first_events(Scenario scenario, Dispatcher dispatcher, float start_time) {
        System.out.println("MICRO register_first_events");

    }

    @Override
    public void register_commodity(Link link, Commodity comm, Subnetwork subnet) throws OTMException {
        System.out.println("MICRO register_commodity");

    }

    @Override
    public void validate(Link link, OTMErrorLog errorLog) {
        System.out.println("MICRO validate");

    }

    @Override
    public void reset(Link link) {
        System.out.println("MICRO reset");

    }

    @Override
    public void build(Link link) {
        System.out.println("MICRO build");

    }

    @Override
    public AbstractLaneGroup create_lane_group(Link link, Side side, FlowDirection flowdir, Float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs) {
        System.out.println("MICRO create_lane_group");
        return null;
    }

    @Override
    public AbstractLinkInfo get_link_info(Link link) {
        System.out.println("MICRO get_link_info");
        return null;
    }

    @Override
    public AbstractSource create_source(Link origin, DemandProfile demand_profile, Commodity commodity, Path path) {

        System.out.println("MICRO create_source");
        return null;
    }
}
