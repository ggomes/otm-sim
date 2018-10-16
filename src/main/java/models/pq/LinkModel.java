/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.pq;

import common.*;
import error.OTMErrorLog;
import error.OTMException;
import runner.Scenario;

import java.util.*;

public class LinkModel extends AbstractLinkModel {

    ////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public LinkModel(common.Link link){
        super(link);
        myPacketClass = models.pq.PacketLaneGroup.class;
    }

    ////////////////////////////////////////////
    // implementation
    ///////////////////////////////////////////

    @Override
    public void set_road_param(jaxb.Roadparam r, float sim_dt_sec) {
        // send parameters to lane groups
        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values())
            lg.set_road_params(r);
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
    }

    @Override
    public void reset() {
        System.out.println("IMPLEMENT THIS");
    }

//    @Override
//    public float get_ff_travel_time() {
//
//        // returns the maximum of the transit times of all of the lanegroup
//        float s = 0f;
//        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values() )
//            s = Math.max( s , ((LaneGroup)lg).transit_time_sec );
//        return s;
//    }

//    @Override
//    public float get_capacity_vps(){
//        // return s the sum of the capacities of all of the lanegroups
//        float s = 0f;
//        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values() )
//            s += ((LaneGroup)lg).saturation_flow_rate_vps;
//        return s;
//    }

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

    ////////////////////////////////////////////
    // update
    ///////////////////////////////////////////

    /**
     * A packet arrives at this link. The packet contains models.ctm/models.ctm.pq/models.ctm.micro vehicles.
     * It is tagged with the road connection that it comes along.
     * Vehicles do not know their next_link. It is assumed that the packet fits in this link.
     * (ie in the lanegroups accessed by the road connection)
     * 1. convert the packet to models.ctm.micro, models.ctm.pq, or models.ctm. This involves memory kept in the lanegroup.
     * 2. tag it with next_link and target lanegroups.
     * 3. add the packet to this lanegroup.
     */
//    @Override
//    public void add_native_vehicle_packet(float timestamp, PacketLink vp) throws OTMException {
//
//        if(vp.isEmpty())
//            return;
//
//        if(link.is_sink){
//            try {
//                AbstractPacketLaneGroup vplg = (AbstractPacketLaneGroup) myPacketClass.newInstance();
//                vplg.target_lanegroups = null;
//                vplg.add_link_packet(vp,true);
//                AbstractLaneGroup join_lanegroup = vp.arrive_to_lanegroups.iterator().next();
//                join_lanegroup.add_native_vehicle_packet(timestamp,vplg);
//            } catch (InstantiationException e) {
//                e.printStackTrace();
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();
//            }
//            return;
//        }
//
//        // tag the packet with next_link and target_lanegroups
//        Map<Long, AbstractPacketLaneGroup> lanegroup_packets = link.packet_splitter.split_packet(models.pq.PacketLaneGroup.class,vp);
//
//        // process each lanegroup packet
//        for(Map.Entry<Long, AbstractPacketLaneGroup> e : lanegroup_packets.entrySet()){
//
//            Long outlink_id = e.getKey();
//            AbstractPacketLaneGroup lanegroup_packet = e.getValue();
//
//            if(lanegroup_packet.isEmpty())
//                continue;
//
//            lanegroup_packet.target_lanegroups = link.outlink2lanegroups.get(outlink_id);
//
//            if(lanegroup_packet.target_lanegroups==null) {
//                String str = "target_lanegroups==null.\n";
//                str += "This may be an error in split ratios. There is no access from link " + link.getId() + " to " +
//                        "link " + outlink_id+ ". A possible cause is that there is a positive split ratio between " +
//                        "these two links.";
//                throw new OTMException(str);
//            }
//
//            // candidates lanegroups are those where the packet has arrived
//            // intersected with those that can reach the outlink
//            // This can be removed if there is a model for "changing lanes" to another lanegroup
//            Set<AbstractLaneGroup> candidate_lanegroups = OTMUtils.intersect( vp.arrive_to_lanegroups , lanegroup_packet.target_lanegroups );
//
//            if(candidate_lanegroups.isEmpty()) {
//                // in this case the vehicle has arrived to lanegroups for which there is
//                // no connection to the out link.
//                // With lane changing implemented, this vehicle would then have to
//                // change lanes (lanegroups) over the length of the link.
//                // For now, just switch it to one of the connecting lanegroups.
//
////                throw new OTMException("candidate_lanegroups.isEmpty(): in link " + link.getId() + ", vehicle arrived to lanegroups " +
////                        vpb.arrive_to_lanegroups + " with target lanegroup " + target_lanegroups);
//                candidate_lanegroups = link.outlink2lanegroups.get(outlink_id);
//            }
//
//            // choose the best one
//            AbstractLaneGroup join_lanegroup = AbstractLaneGroup.choose_best_lanegroup(candidate_lanegroups);
//
//            // if all candidates are full, then choose one that is closest and not full
//            if(join_lanegroup==null) {
//
//                join_lanegroup = choose_closest_that_is_not_full(vp.arrive_to_lanegroups,candidate_lanegroups,lanegroup_packet.target_lanegroups);
//
//                // put lane change requests on the target lane groups
//                add_lane_change_request(timestamp,lanegroup_packet,join_lanegroup,lanegroup_packet.target_lanegroups,Queue.Type.transit);
//            }
//
//            // add the packet to it
//            join_lanegroup.add_native_vehicle_packet(timestamp,lanegroup_packet);
//
//        }
//
//    }

    protected void process_lane_change_request(float timestamp,LaneChangeRequest x) throws OTMException {

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
