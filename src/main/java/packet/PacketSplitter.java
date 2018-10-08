/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package packet;

import commodity.Commodity;
import commodity.Path;
import common.*;
import error.OTMErrorLog;
import keys.KeyCommPathOrLink;
import utils.OTMUtils;

import java.util.*;
import java.util.stream.Collectors;

public class PacketSplitter {

    public Link link;
    public Map<Long,Set<AbstractLaneGroup>> outputlink_targetlanegroups;
    public Map<Long, SplitInfo> commodity2split;

    //////////////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////////////

    public PacketSplitter(Link link){
        this.link = link;
        Set<Link> next_links = link.get_next_links();
        commodity2split = new HashMap<>();
        for(Commodity c : link.commodities) {
            Set<Link> comm_next_links = OTMUtils.intersect(c.all_links,next_links);
            Long trivial_next_link = comm_next_links.size()==1 ? comm_next_links.iterator().next().getId() : null;
            commodity2split.put(c.getId(), new SplitInfo(trivial_next_link));
        }

        // populate outputlink_targetlanegroups
        outputlink_targetlanegroups = new HashMap<>();
        for(Link outlink : link.end_node.out_links.values())
            outputlink_targetlanegroups.put(outlink.getId(),link.long_lanegroups.values().stream()
                                                            .filter(z->z.link_is_link_reachable(outlink.getId()))
                                                            .collect(Collectors.toSet()) );

    }

    // called from EventSplitChange cascade, during initialization
    public void set_splits(long commodity_id,Map<Long,Double> outlink2value){
        if(commodity2split.containsKey(commodity_id))
            commodity2split.get(commodity_id).set_splits(outlink2value);
    }

    public void validate(OTMErrorLog errorLog){
//
//        // split info has information for downstream links for each commodity
//        Collection<Link> next_links = link.get_next_links();
//        for(Commodity commodity : link.commodities){
//            Collection<Link> comm_next_links = OTMUtils.intersect(commodity.all_links(),next_links);

            // there should be information available if there is a split for this commodity
            // NOTE: CANT DO THIS BEFORE INITIALIZATION!!
//            if(comm_next_links.size()>1){
//                if(!commodity2split.containsKey(commodity.getId())) {
//                    scenario.error_log.addError("link " + link.id + ": !target_lanegroup_splits.containsKey(commodity_id)");
//                } else {
//                    SplitInfo splitInfo = commodity2split.get(commodity.getId());
//                    if (splitInfo == null || splitInfo.link_cumsplit == null)
//                        scenario.error_log.addError("missing splits on link " + link.getId() + " for commodity " + commodity.getId());
//                    else {
//                        Set<Long> info_links = splitInfo.link_cumsplit.stream().map(x -> x.link_id).collect(toSet());
//                        Set<Long> next_link_ids = comm_next_links.stream().map(x -> x.getId()).collect(toSet());
//                        if (!info_links.equals(next_link_ids))
//                            scenario.error_log.addError("!info_links.equals(next_link_ids)");
//                    }
//                }
//            }

//        }

        // check that all commodities have splitinfo
//        if(link.lanegroups.size()>1)
//            for(Commodity commodity : link.commodities)
//                if(!commodity2split.containsKey(commodity.getId()))
//                    scenario.error_log.addError("link " + link.id + ": !target_lanegroup_splits.containsKey(commodity_id)");

        // check that all output links in subnetwork are represented
//        for(Map.Entry e : commodity2split.entrySet()){
//            long commodity_id = (Long) e.getKey();
//            SplitInfo splitinfo = (SplitInfo) e.getValue();
//            Commodity commodity = scenario.commodities.get(commodity_id);
//            if(commodity==null)
//                scenario.error_log.addError("commodity==null");

//            // all links immediately downstream of this link
//            Set<Long> dwn_links_ids = this.link.end_node.outputs.values()
//                    .stream()
//                    .map(x->x.id)
//                    .collect(Collectors.toSet());

//            // subnetwork links
//            Set<Long> subnet_ids = commodity.all_links().stream().map(x->x.getId()).collect(Collectors.toSet());
//
//            // keep only those downstream links that are also in the subnetwork
//            dwn_links_ids.retainAll(subnet_ids);
//
//            // all split outlinks must be down links
//            Set<Long> split_outlinks = outputlink_targetlanegroups.keySet();
//            if(!dwn_links_ids.containsAll(split_outlinks))
//                scenario.error_log.addError("!dwn_links_ids.containsAll(split_outlinks)");
//        }
    }

    //////////////////////////////////////////////////////////////
    // public
    //////////////////////////////////////////////////////////////

    // split a packet according to downstream link.
    // for pathfull commodities, the next link is trivial. For pathless, it is sampled from the split ratios
    public Map<Long, AbstractPacketLaneGroup> split_packet(Class packet_class,PacketLink vp){

        // initialize lanegroup_packets
        Map<Long, AbstractPacketLaneGroup> lanegroup_packets = new HashMap<>();

        boolean has_macro = !vp.no_macro();
        boolean has_micro = !vp.no_micro();

        // process the macro state
        if(has_macro) {

            for (Map.Entry<KeyCommPathOrLink, Double> e : vp.state2vehicles.entrySet()) {

                KeyCommPathOrLink key = e.getKey();
                Double vehicles = e.getValue();

                // pathfull case
                if (key.isPath) {

                    // TODO: Cache this
                    Path path = (Path) link.network.scenario.subnetworks.get(key.pathOrlink_id);
                    Long outlink_id = path.get_link_following(link).getId();
                    add_to_lanegroup_packets(packet_class,lanegroup_packets,outlink_id,key,vehicles);
                }

                // pathless case
                else {

                    SplitInfo splitinfo = commodity2split.get(key.commodity_id);

                    if(splitinfo.sole_downstream_link!=null){
                        Long outlink_id = splitinfo.sole_downstream_link;
                        add_to_lanegroup_packets(packet_class, lanegroup_packets, outlink_id,
                                new KeyCommPathOrLink(key.commodity_id, outlink_id, false),
                                vehicles );
                    }

                    else {
                        for (Map.Entry<Long, Double> e2 : splitinfo.outlink2split.entrySet()) {
                            Long outlink_id = e2.getKey();
                            Double split = e2.getValue();
                            add_to_lanegroup_packets(packet_class, lanegroup_packets, outlink_id,
                                    new KeyCommPathOrLink(key.commodity_id, outlink_id, false),
                                    vehicles * split);
                        }
                    }
                }
            }
        }

        // process the micro state
        if(has_micro){
            for(AbstractVehicle vehicle : vp.vehicles){

                KeyCommPathOrLink key = vehicle.get_key();

                // pathfull case
                Long outlink_id;
                if(key.isPath){
                    // TODO: Cache this
                    Path path = (Path) link.network.scenario.subnetworks.get(key.pathOrlink_id);
                    outlink_id = path.get_link_following(link).getId();
                    add_to_lanegroup_packets(packet_class,lanegroup_packets,outlink_id,key,vehicle);
                }

                // pathless case
                else {
                    outlink_id = commodity2split.get(key.commodity_id).sample_output_link();
                    vehicle.set_next_link_id(outlink_id);
                    add_to_lanegroup_packets(packet_class,lanegroup_packets,outlink_id ,
                            new KeyCommPathOrLink(key.commodity_id, outlink_id, false),
                            vehicle);
                }
            }
        }

        return lanegroup_packets;
    }

    public static AbstractPacketLaneGroup cast_packet_null_splitter(Class packet_class,PacketLink vp,Long outlink_id){

        AbstractPacketLaneGroup split_packet;
        try {
            split_packet = (AbstractPacketLaneGroup) packet_class.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }

        boolean has_macro = !vp.no_macro();
        boolean has_micro = !vp.no_micro();

        // process the macro state
        if(has_macro) {
            for (Map.Entry<KeyCommPathOrLink, Double> e : vp.state2vehicles.entrySet()) {
                KeyCommPathOrLink key = e.getKey();
                Double vehicles = e.getValue();
                if (key.isPath || outlink_id==null)  // null occurs for sinks
                    split_packet.add_macro(key,vehicles);
                else
                    split_packet.add_macro(new KeyCommPathOrLink(key.commodity_id, outlink_id, false),vehicles);
            }
        }

        // process the micro state
        if(has_micro){
            for(AbstractVehicle vehicle : vp.vehicles){
                KeyCommPathOrLink key = vehicle.get_key();
                // NOTE: We do not update the next link id when it is null. This happens in
                // sinks. This means that the state in a sink needs to be interpreted
                // differently, which must be accounted for everywhere.
                if(key.isPath || outlink_id==null)
                    split_packet.add_micro(key,vehicle);
                else {
                    vehicle.set_next_link_id(outlink_id);
                    split_packet.add_micro(new KeyCommPathOrLink(key.commodity_id, outlink_id, false),vehicle);
                }
            }
        }

        return split_packet;
    }

    public Long get_nextlink_for_key(KeyCommPathOrLink key){

        // this is a sink, no next link
        if(link.is_sink)
            return null;

        // get next link id
        Long outlink_id;
        if(key.isPath) {
            // TODO: Cache this
            Path path = (Path) link.network.scenario.subnetworks.get(key.pathOrlink_id);
            outlink_id = path.get_link_following(link).getId();
        }
        // otherwise use split ratios
        else {
            outlink_id = commodity2split.get(key.commodity_id).sample_output_link();
        }
        return outlink_id;
    }

    // return outlink to split
    public Map<Long,Double> get_splits_for_commodity(Long comm_id){
        if(!commodity2split.containsKey(comm_id))
            return null;
        return commodity2split.get(comm_id).outlink2split;
    }

    //////////////////////////////////////////////////////////////
    // private
    //////////////////////////////////////////////////////////////

    private static void add_to_lanegroup_packets(Class packet_class,Map<Long, AbstractPacketLaneGroup> lanegroup_packets,Long outlink_id,KeyCommPathOrLink key,Double vehicles){
        try {
            AbstractPacketLaneGroup split_packet;
            if(lanegroup_packets.containsKey(outlink_id)){
                split_packet = lanegroup_packets.get(outlink_id);
            } else {
                split_packet = (AbstractPacketLaneGroup) packet_class.newInstance();
                lanegroup_packets.put(outlink_id,split_packet);
            }
            split_packet.add_macro(key,vehicles);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static void add_to_lanegroup_packets(Class packet_class,Map<Long, AbstractPacketLaneGroup> lanegroup_packets,Long outlink_id,KeyCommPathOrLink key,AbstractVehicle vehicle){
        try {
            AbstractPacketLaneGroup split_packet;
            if(lanegroup_packets.containsKey(outlink_id)){
                split_packet = lanegroup_packets.get(outlink_id);
            } else {
                split_packet = (AbstractPacketLaneGroup) packet_class.newInstance();
                lanegroup_packets.put(outlink_id,split_packet);
            }
            split_packet.add_micro(key,vehicle);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
