/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package common;

import commodity.Commodity;
import commodity.Subnetwork;
import error.OTMErrorLog;
import error.OTMException;
import packet.AbstractPacketLaneGroup;
import packet.PacketLink;
import packet.PacketSplitter;
import runner.Scenario;

import java.util.*;

public abstract class AbstractLinkModel {

    public Class myPacketClass;
    public common.Link link;

    //////////////////////////////////////////////////////////////
    // abstract methods
    //////////////////////////////////////////////////////////////

    abstract public void set_road_param(jaxb.Roadparam r, float sim_dt_sec);
    abstract public void validate(OTMErrorLog errorLog);
    abstract public void reset();
    abstract public float get_ff_travel_time(); // seconds
    abstract public float get_capacity_vps();   // vps
    abstract public Map<AbstractLaneGroup,Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups);

    //////////////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////////////

    public AbstractLinkModel(common.Link link){
        this.link = link;
    }

    public void register_commodity(Commodity comm, Subnetwork subnet) throws OTMException {

        if(comm.pathfull) {
            for (AbstractLaneGroup lg : link.lanegroups_flwdn.values())
                lg.add_state(comm.getId(), subnet.getId(), true);
        }

        else {

            // for pathless/sink, next link id is same as this id
            if (link.is_sink) {
                for (AbstractLaneGroup lg : link.lanegroups_flwdn.values())
                    lg.add_state(comm.getId(), link.getId(), false);

            } else {

                // for pathless non-sink, add a state for each next link in the subnetwork
                for( Long next_link_id : link.outlink2lanegroups.keySet()  ){
                    if (!subnet.has_link_id(next_link_id))
                        continue;
                    for (AbstractLaneGroup lg : link.lanegroups_flwdn.values())
                        lg.add_state(comm.getId(), next_link_id, false);
                }
            }
        }

    }

    public void initialize(Scenario scenario) throws OTMException {
        // allocate state for each lanegroup in this link
        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values() ){
            lg.allocate_state();
        }
    }

    //////////////////////////////////////////////////////////////
    // public
    //////////////////////////////////////////////////////////////

    public void add_vehicle_packet(float timestamp, PacketLink vp) throws OTMException {

        if(vp.isEmpty())
            return;

        // sink or many-to-one
        // this implies that next-link is trivial
        // and (for now) target_lanegroup is trivial
        if(link.packet_splitter==null){
            // if sink, encode by using current link id as nextlink.
            Long outlink_id = link.is_sink ? link.getId() : link.end_node.out_links.values().iterator().next().getId();
            AbstractPacketLaneGroup packet = PacketSplitter.cast_packet_null_splitter(myPacketClass,vp,outlink_id);
            AbstractLaneGroup join_lanegroup = vp.arrive_to_lanegroups.iterator().next();
            join_lanegroup.add_native_vehicle_packet(timestamp,packet);
            return;
        }

        // tag the packet with next_link and target_lanegroups
        Map<Long, AbstractPacketLaneGroup> split_packets = link.packet_splitter.split_packet(myPacketClass,vp);

        // process each split packet
        for(Map.Entry<Long, AbstractPacketLaneGroup> e : split_packets.entrySet()){

            Long outlink_id = e.getKey();
            AbstractPacketLaneGroup split_packet = e.getValue();

            if(split_packet.isEmpty())
                continue;

            split_packet.target_lanegroups = link.outlink2lanegroups.get(outlink_id);

            if(split_packet.target_lanegroups==null)
                throw new OTMException(String.format("target_lanegroups==null.\nThis may be an error in split ratios. " +
                        "There is no access from link " + link.getId() + " to " +
                        "link " + outlink_id+ ". A possible cause is that there is " +
                        "a positive split ratio between these two links."));

            Set<AbstractLaneGroup> candidate_lanegroups = vp.arrive_to_lanegroups;

            // split the split_packet amongst the candidate lane groups.
            // then add them
            if(candidate_lanegroups.size()==1) {
                AbstractLaneGroup laneGroup = candidate_lanegroups.iterator().next();
                laneGroup.add_native_vehicle_packet(timestamp, split_packet);
            } else {
                for (Map.Entry<AbstractLaneGroup, Double> ee : lanegroup_proportions(candidate_lanegroups).entrySet()) {
                    AbstractLaneGroup laneGroup = ee.getKey();
                    Double prop = ee.getValue();
                    if (prop <= 0d)
                        continue;
                    if (prop==1d)
                        laneGroup.add_native_vehicle_packet(timestamp, split_packet );
                    else
                        laneGroup.add_native_vehicle_packet(timestamp, split_packet.times(prop));
                }
            }

//            // TODO: FOR MESO and MICRO MODELS, CHECK THAT THERE IS AT LEAST 1 VEHICLE WORTH OF SUPPLY.
//
//            // if all candidates are full, then choose one that is closest and not full
//            if(join_lanegroup==null) {
//
//                join_lanegroup = choose_closest_that_is_not_full(vp.arrive_to_lanegroups,candidate_lanegroups,split_packet.target_lanegroups);
//
//                // put lane change requests on the target lane groups
//                // TODO: REDO THIS
////                add_lane_change_request(timestamp,lanegroup_packet,join_lanegroup,lanegroup_packet.target_lanegroups,Queue.Type.transit);
//            }

            // add the packet to it

        }

    }

    public float get_max_vehicles(){
        return (float) link.lanegroups_flwdn.values().stream().map(x->x.max_vehicles).mapToDouble(i->i).sum();
    }

    //////////////////////////////////////////////////////////////
    // private
    //////////////////////////////////////////////////////////////

//    private AbstractLaneGroup choose_closest_that_is_not_full(Set<AbstractLaneGroup> arrive_to_lanegroups,Set<AbstractLaneGroup> candidate_lanegroups,Set<AbstractLaneGroupLongitudinal> target_lanegroups) throws OTMException {
//
//        // these will be selected from among the lanegroups that do not directly connect to
//        // the output link.
//        List<AbstractLaneGroup> second_best_candidates = new ArrayList(OTMUtils.setminus(arrive_to_lanegroups,candidate_lanegroups));
//
//        // this should not be empty. Otherwise the assumption that the link was checked for space is vuilated.
//        if(second_best_candidates.isEmpty())
//            throw new OTMException("This should not happen.");
//
//        // from these select the one that is closest to the destination lanegroups (ie minimizes lane changes)
//
//        // find the range of lanes of the target lanegroups
//        List<Integer> target_lanes = target_lanegroups.stream()
//                .map(x->x.lanes)
//                .flatMap(x->x.stream())
//                .collect(toList());
//        Integer min_lane = target_lanes.stream().mapToInt(x->x).min().getAsInt();
//        Integer max_lane = target_lanes.stream().mapToInt(x->x).max().getAsInt();
//
//        // compute the distance of each second best candidate to the targets
//        List<Integer> distance_to_target = second_best_candidates.stream()
//                .map(x->x.distance_to_lanes(min_lane,max_lane))
//                .collect(toList());
//
//        // find the index of the smallest distance
//        int index = IntStream.range(0,distance_to_target.size()).boxed()
//                .min(comparingInt(distance_to_target::get))
//                .get();
//
//        // pick that lanegroup to join
//        return second_best_candidates.get(index);
//    }

//    private void add_lane_change_request(float timestamp, AbstractPacketLaneGroup packet, AbstractLaneGroup from_lanegroup, Set<AbstractLaneGroup> to_lanegroups, Queue.Type queue_type) throws OTMException{
//
//        // the packet should contain a single models.ctm.pq vehicle
//        if(packet.vehicles.isEmpty() || packet.vehicles.size()!=1)
//            throw new OTMException("This is weird.");
//
//        AbstractVehicle abs_vehicle = packet.vehicles.iterator().next();
//        if( !(abs_vehicle instanceof Vehicle) )
//            throw new OTMException("This is weird.");
//
//        Vehicle vehicle = (Vehicle) abs_vehicle;
//
//        // define from queue
//        Queue from_queue = null;
//        switch(queue_type){
//            case transit:
//                from_queue = ((LaneGroup)from_lanegroup).transit_queue;
//                break;
//            case waiting:
//                from_queue = ((LaneGroup)from_lanegroup).waiting_queue;
//                break;
//        }
//
//        // create the request and add it to the destination lanegroup
//        for(AbstractLaneGroup lg : to_lanegroups) {
//            Queue to_queue = null;
//            switch(queue_type){
//                case transit:
//                    to_queue = ((LaneGroup) lg).transit_queue;
//                    break;
//                case waiting:
//                    to_queue = ((LaneGroup) lg).waiting_queue;
//                    break;
//            }
//            to_queue.submit_lane_change_request( new LaneChangeRequest(timestamp, vehicle, from_queue,to_queue));
//        }
//
//        // vehicle is requesting lane change
//        vehicle.waiting_for_lane_change = true;
//
//    }

}
