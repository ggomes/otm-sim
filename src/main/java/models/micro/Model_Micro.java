package models.micro;

import commodity.Commodity;
import commodity.Path;
import commodity.Subnetwork;
import common.AbstractSource;
import common.AbstractVehicle;
import common.Link;
import common.RoadConnection;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import geometry.FlowDirection;
import geometry.Side;
import jaxb.OutputRequest;
import keys.KeyCommPathOrLink;
import models.AbstractLaneGroup;
import models.AbstractVehicleModel;
import models.VehicleSource;
import output.AbstractOutput;
import output.InterfaceVehicleListener;
import output.animation.AbstractLinkInfo;
import profiles.DemandProfile;
import runner.Scenario;

import java.util.*;

public class Model_Micro extends AbstractVehicleModel {

    public float dt;

    public Model_Micro(String name, boolean is_default, Float dt) {
        super(name, is_default);
        this.dt = dt==null ? -1 : dt;
        myPacketClass = models.micro.PacketLaneGroup.class;
    }

    //////////////////////////////////////////////////
    // load
    //////////////////////////////////////////////////

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

    //////////////////////////////////////////////////
    // factory
    //////////////////////////////////////////////////

    @Override
    public AbstractVehicle create_vehicle(KeyCommPathOrLink key, Set<InterfaceVehicleListener> vehicle_event_listeners) {
        return new models.micro.Vehicle(key,vehicle_event_listeners);
    }

    @Override
    public AbstractOutput create_output_object(Scenario scenario, String prefix, String output_folder, OutputRequest jaxb_or) throws OTMException {
        System.out.println("MICRO create_output_object");
        return null;
    }

    // SIMILAR AS PQ
    @Override
    public AbstractLaneGroup create_lane_group(Link link, Side side, FlowDirection flowdir, Float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs) {
        return new models.micro.LaneGroup(link,side,flowdir,length,num_lanes,start_lane,out_rcs);
    }

    @Override
    public AbstractLinkInfo get_link_info(Link link) {
        System.out.println("MICRO get_link_info");
        return null;
    }

    //////////////////////////////////////////////////
    // run
    //////////////////////////////////////////////////

    // SAME AS PQ
    @Override
    public Map<AbstractLaneGroup, Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups) {
        // put the whole packet i the lanegroup with the most space.
        Optional<? extends AbstractLaneGroup> best_lanegroup = candidate_lanegroups.stream()
                .max(Comparator.comparing(AbstractLaneGroup::get_space_per_lane));

        if(best_lanegroup.isPresent()) {
            Map<AbstractLaneGroup,Double> A = new HashMap<>();
            A.put(best_lanegroup.get(),1d);
            return A;
        } else
            return null;
    }

    @Override
    public void register_first_events(Scenario scenario, Dispatcher dispatcher, float start_time) {
        System.out.println("MICRO register_first_events");

    }

}
