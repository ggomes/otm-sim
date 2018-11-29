package models.pq;

import commodity.Commodity;
import commodity.Subnetwork;
import common.AbstractLaneGroup;
import common.Link;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import models.AbstractDiscreteEventModel;
import runner.Scenario;

import java.util.*;

public class Model_PQ extends AbstractDiscreteEventModel {

    public Model_PQ(Set<Link> links, String name) {
        super(links, name);
        myPacketClass = models.pq.PacketLaneGroup.class;
    }

    @Override
    public void build(Link link) {

    }

    @Override
    public void register_first_events(Scenario scenario, Dispatcher dispatcher, float start_time) {

    }

    @Override
    public void register_commodity(Link link, Commodity comm, Subnetwork subnet) throws OTMException {

    }

    @Override
    public void set_road_param(Link link, jaxb.Roadparam r, float sim_dt_sec) {
        // send parameters to lane groups
        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values())
            lg.set_road_params(r);
    }

    @Override
    public void validate(Link link,OTMErrorLog errorLog) {
    }

    @Override
    public void reset(Link link) {
        System.out.println("IMPLEMENT THIS");
    }

    @Override
    public Map<AbstractLaneGroup,Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups) {

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


    protected void process_lane_change_request(Link link,float timestamp,LaneChangeRequest x) throws OTMException {

        if(x==null)
            return;

        // the vehicle must be in to_queue
        if(x.requester.my_queue!=x.from_queue)
            return;

        // move the vehicle to the destination queue
        x.requester.move_to_queue(timestamp,x.to_queue);

        // remove all of its requests by this vehicle in this link
        for (AbstractLaneGroup lanegroup : link.lanegroups_flwdn.values()) {
            LaneGroup lg = (LaneGroup) lanegroup;
            lg.transit_queue.remove_lane_change_requests_for_vehicle(x.requester);
            lg.waiting_queue.remove_lane_change_requests_for_vehicle(x.requester);
        }

        // vehicle is not longer changing lanes
        x.requester.waiting_for_lane_change = false;

    }


}
