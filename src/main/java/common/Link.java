/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package common;

import actuator.AbstractActuator;
import actuator.InterfaceActuatorTarget;
import commodity.Commodity;
import error.OTMErrorLog;
import error.OTMException;
import geometry.RoadGeometry;
import geometry.Side;
import jaxb.Points;
import jaxb.Roadparam;
import keys.KeyCommPathOrLink;
import models.AbstractLaneGroup;
import models.AbstractModel;
import output.PathTravelTime;
import packet.AbstractPacketLaneGroup;
import packet.PacketLink;
import runner.InterfaceScenarioElement;
import runner.RunParameters;
import runner.Scenario;
import runner.ScenarioElementType;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

public class Link implements InterfaceScenarioElement, InterfaceActuatorTarget {

    public enum RoadType {none,onramp,offramp,freeway,arterial,hov,interconnect,source,sink,lightrail}

    // basics ........................................
    protected final long id;
    public Network network;
    public final float length;          // meters
    public final int full_lanes;
    public common.Node start_node;
    public common.Node end_node;
    public boolean is_source;
    public boolean is_sink;

    // parameters .........................................
    public RoadType road_type;
    public RoadGeometry road_geom;
    public Roadparam road_param;        // for the sake of writing again to jaxb
    public List<Point> shape;           // not used by otm-sim

    // model .............................................
    public AbstractModel model;

    // lanegroups ......................................

    // Longitudinal lanegroups: flow exits from the bottom edge.
    // There are stay lanegroups and downstream addlanes
    public Map<Long, AbstractLaneGroup> lanegroups_flwdn;

    // Lateral lanegroups: all flow exits laterally. These are the upstream addlanes.
    public AbstractLaneGroup lanegroup_flwside_in;
    public AbstractLaneGroup lanegroup_flwside_out;

    // downstream lane count -> lane group
    public Map<Integer, AbstractLaneGroup> dnlane2lanegroup;

    // routing information ...............................

    // map from path id (uses this link) to next link id (exits this link)
    // populated by ScenarioFactory.create_scenario
    public Map<Long,Link> path2outlink;

    // outlink -> lanegroups from which outlink is reachable
    // built by Network contstructor.
    public Map<Long,Set<AbstractLaneGroup>> outlink2lanegroups;

    // splits
    // populated by ScenarioFactory.create_scenario
    public Map<Long, SplitInfo> commodity2split;

    // demands ............................................
    // populated by DemandProfile constructor
    public Set<AbstractSource> sources;

    // actuators .........................................
    public actuator.ActuatorRampMeter ramp_meter;
    public actuator.ActuatorFD actuator_fd;

    // output ...........................................
    public Set<PathTravelTime> travel_timers;

    ///////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public Link(Network network, jaxb.Roadparam rp, long id, float length, int full_lanes, Node start_node, Node end_node) throws OTMException {

        if (start_node == null)
            throw new OTMException("Unknown start node id in link " + id);

        if (end_node == null)
            throw new OTMException("Unknown end node id in link " + id);

        // basics ..............................
        this.id = id;
        this.network = network;
        this.length = length;
        this.full_lanes = full_lanes;
        this.start_node = start_node;
        this.end_node = end_node;

        // source and sink. this is set later by the network
        this.is_source = true;
        this.is_sink = true;

        // node io
        this.start_node.add_output_link(this);
        this.end_node.add_input_link(this);

        // parameters .........................................
        this.road_param = rp;

        // shape
        this.shape = new ArrayList<>();

        // lanegroups ......................................
        lanegroups_flwdn = new HashMap<>();
        dnlane2lanegroup = new HashMap<>();

        // routing ............................................
        path2outlink = new HashMap<>();
        outlink2lanegroups = new HashMap<>();
        commodity2split = new HashMap<>();

        // demands ............................................
        sources = new HashSet<>();

        // output ...........................................
        travel_timers = new HashSet<>();

    }

    public Link(Network network, jaxb.Roadparam rp, long id, float length, int full_lanes, Node start_node, Node end_node, RoadGeometry rg, Link.RoadType rt,Points jpoints) throws OTMException {

        this(network,rp,id,length,full_lanes,start_node,end_node);

        // parameters .........................................
        this.road_type = rt==null ? RoadType.none : rt;
        this.road_geom = rg;

        // shape
        if (jpoints != null && jpoints.getPoint() != null)
            for (jaxb.Point jpoint : jpoints.getPoint())
                shape.add(new Point(jpoint.getX(), jpoint.getY()));
        else if (start_node.xcoord != null && start_node.ycoord != null && end_node.xcoord != null && end_node.ycoord != null) {
            shape.add(new Point(start_node.xcoord, start_node.ycoord));
            shape.add(new Point(end_node.xcoord, end_node.ycoord));
        }

    }

    public void delete(){
        network = null;
        start_node = null;
        end_node = null;
        if(lanegroups_flwdn !=null)
            lanegroups_flwdn.values().forEach(lg->lg.delete());
        lanegroups_flwdn = null;
        lanegroup_flwside_out.delete();
        lanegroup_flwside_in.delete();
        lanegroup_flwside_out = null;
        lanegroup_flwside_in = null;
        dnlane2lanegroup = null;
        path2outlink = null;
        outlink2lanegroups = null;
        commodity2split = null;
        if(sources!=null)
            sources.forEach(s->s.delete());
        sources = null;
        road_type = null;
        road_geom = null;
        model = null;
        travel_timers = null;
        shape = null;
    }

    public void add_travel_timer(PathTravelTime x){
        travel_timers.add(x);
    }

    public void set_long_lanegroups(Collection<AbstractLaneGroup> lgs) {

        lanegroups_flwdn = new HashMap<>();
        dnlane2lanegroup = new HashMap<>();

        if(lgs==null || lgs.isEmpty())
            return;

        // lanegroups
        for(AbstractLaneGroup lg : lgs)
            lanegroups_flwdn.put(lg.id,lg);

        // dnlane2lanegroup
        for (AbstractLaneGroup lg : lgs)
            for (int lane=lg.start_lane_dn;lane<lg.start_lane_dn+lg.num_lanes;lane++)                       // iterate through dn lanes
                dnlane2lanegroup.put(lane, lg);
    }

    public void populate_outlink2lanegroups(){

        if(is_sink)
            return;

        outlink2lanegroups = new HashMap<>();
        for(Link outlink : end_node.out_links.values())
            outlink2lanegroups.put(outlink.getId(),lanegroups_flwdn.values().stream()
                    .filter(lg->lg.link_is_link_reachable(outlink.getId()))
                    .collect(Collectors.toSet()) );
    }

    public void populate_commodity2split(Collection<Commodity> commodities){
        Long trivial_next_link = outlink2lanegroups.size() == 1 ? outlink2lanegroups.keySet().iterator().next() : null;
        commodity2split = new HashMap<>();
        for(Commodity c : commodities)
            commodity2split.put(c.getId(), new SplitInfo(trivial_next_link));
    }

//    public void set_lat_lanegroups(Collection<AbstractLaneGroupLateral> lgs) {
//        lat_lanegroups = new HashMap<>();
//        if(lgs==null || lgs.isEmpty())
//            return;
//        for(AbstractLaneGroupLateral lg : lgs)
//            lat_lanegroups.put(lg.id,lg);
//    }

    // called from EventSplitChange cascade, during initialization
    public void set_splits(long commodity_id,Map<Long,Double> outlink2value){
        if(commodity2split.containsKey(commodity_id))
            commodity2split.get(commodity_id).set_splits(outlink2value);
    }

    public void set_model(AbstractModel newmodel) throws OTMException {

        if (model==null){
            this.model = newmodel;
            return;
        }

        if(model.is_default && !newmodel.is_default) {
            this.model = newmodel;
            return;
        }

        if(model.is_default && newmodel.is_default)
            throw new OTMException("Multiple default models");

        if(!model.is_default && !newmodel.is_default)
            throw new OTMException("ModelType multiply assigned for link " + this.id);
    }

//    public FlowAccumulatorSet request_flow_accumulator_set(Long commodity_id){
//        FlowAccumulatorSet fas = new FlowAccumulatorSet();
//        for(AbstractLaneGroup lg : lanegroups.values())
//            fas.add_flow_accumulator(lg.request_flow_accumulator(commodity_id));
//        return fas;
//    }

    public void validate(OTMErrorLog errorLog){
        if( length<=0 )
            errorLog.addError("link " + id + ": length<=0");
        if( start_node==null )
            errorLog.addError("link " + id + ": start_node==null");
        if( end_node==null )
            errorLog.addError("link " + id + ": end_node==null");
        if( model ==null )
            errorLog.addError("link " + id + ": model==null");
//        if( road_geom ==null )
//            errorLog.addError("link " + id + ": road_geom==null");
        if( dnlane2lanegroup ==null )
            errorLog.addError("link " + id + ": dnlane2lanegroup==null");
        if( lanegroups_flwdn ==null )
            errorLog.addError("link " + id + ": lanegroups==null");

        // check that the road geometry fits the link
        if(this.road_geom!=null){

            // each addlane has length less than the link
            if(road_geom.up_in!=null && road_geom.up_in.length>this.length )
                errorLog.addError("link " + id + ", road_geom.up_in.length > this.length");
            if(road_geom.up_out!=null && road_geom.up_out.length>this.length )
                errorLog.addError("link " + id + ", road_geom.up_out.length > this.length");
            if(road_geom.dn_in!=null && road_geom.dn_in.length>this.length )
                errorLog.addError("link " + id + ", road_geom.dn_in.length > this.length");
            if(road_geom.dn_out!=null && road_geom.dn_out.length>this.length )
                errorLog.addError("link " + id + ", road_geom.dn_out.length > this.length");

            // sum of inside (outside) addlane lengths is less than link length
            if(road_geom.up_in!=null && road_geom.dn_in!=null && road_geom.up_in.length+road_geom.dn_in.length>this.length)
                errorLog.addError("link " + id + ", road_geom.up_in.length+road_geom.dn_in.length>this.length");
            if(road_geom.up_out!=null && road_geom.dn_out!=null && road_geom.up_out.length+road_geom.dn_out.length>this.length)
                errorLog.addError("link " + id + ", road_geom.up_out.length+road_geom.dn_out.length>this.length");
        }

        // all lanes are covered in dnlane2lanegroup
//        if( dnlane2lanegroup !=null) {
//            Set<Integer> range = IntStream.rangeClosed(1,total_lanes).boxed().collect(toSet());
//            Set<Integer> lanes = dnlane2lanegroup.keySet();
//            if (!range.equals(lanes))
//                errorLog.addError("link " + id + ": !range.equals(lanes)");
//        }

        // all lanegroups are represented in dnlane2lanegroup
        if(lanegroups_flwdn !=null && dnlane2lanegroup !=null) {
            Set<Long> A = dnlane2lanegroup.values().stream().map(x->x.id).collect(toSet());
            Set<Long> B = lanegroups_flwdn.values().stream().map(x->x.id).collect(toSet());
            if (!A.equals(B))
                errorLog.addError("link " + id + ": not all lanegroups are represented in dnlane2lanegroup");
        }

        // lanegroups
        if(lanegroups_flwdn !=null)
            lanegroups_flwdn.values().forEach(x->x.validate(errorLog));

        // check that all lanes have a lanegroup
        // WARNING: Assumes no upstream addlanes
//        if(road_geom!=null && dnlane2lanegroup !=null)
//            for(int lane : this.get_exit_lanes() )
//                if(!dnlane2lanegroup.containsKey(lane))
//                    errorLog.addError("link " + id + ": !dnlane2lanegroup.containsKey(lane)");

//        // packet_splitter
//        if(packet_splitter !=null)
//            packet_splitter.validate(errorLog);

        /////////////////////////////////////////////////////////////////
        // BROUGHT OVER FROM PACKETSPLITTER VALIDATION
        /////////////////////////////////////////////////////////////////

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

    public void initialize(Scenario scenario, RunParameters runParams) throws OTMException {
        for(AbstractLaneGroup lg : lanegroups_flwdn.values())
            lg.initialize(scenario,runParams);
    }

    ////////////////////////////////////////////
    // inter-link dynamics
    ///////////////////////////////////////////

    // split a packet according to next links.
    // for pathfull commodities, the next link is the next in the path.
    // For pathless, it is sampled from the split ratios
    // this assigns the state for the split packet but does not yet assign
    // the target road connection. This is done after joining the lane group.
    // This DOES NOT update the keys within the AbstractPacketLaneGroup's
    // and hence they are out of sync with the next link ids (keys in the map)
    public Map<Long, AbstractPacketLaneGroup> split_packet(PacketLink vp){

        // initialize lanegroup_packets
        Map<Long, AbstractPacketLaneGroup> split_packets = new HashMap<>();

        boolean has_macro = !vp.no_macro();
        boolean has_micro = !vp.no_micro();

        // process the macro state
        if(has_macro) {

            for (Map.Entry<KeyCommPathOrLink, Double> e : vp.state2vehicles.entrySet()) {

                KeyCommPathOrLink key = e.getKey();
                Double vehicles = e.getValue();

                // pathfull
                if (key.isPath) {
                    Link next_link = path2outlink.get(key.pathOrlink_id);
                    add_to_lanegroup_packets(split_packets,next_link.getId(),key,vehicles);
                }

                // pathless
                else {

                    SplitInfo splitinfo = commodity2split.get(key.commodity_id);

                    if( is_sink ){
                        add_to_lanegroup_packets(split_packets, id,
                                new KeyCommPathOrLink(key.commodity_id, id, false),
                                vehicles );

                    }

                    else if(splitinfo.sole_downstream_link!=null){
                        Long next_link_id = splitinfo.sole_downstream_link;
                        add_to_lanegroup_packets(split_packets, next_link_id,
                                new KeyCommPathOrLink(key.commodity_id, next_link_id, false),
                                vehicles );
                    }

                    else {
                        for (Map.Entry<Long, Double> e2 : splitinfo.outlink2split.entrySet()) {
                            Long next_link_id = e2.getKey();
                            Double split = e2.getValue();
                            add_to_lanegroup_packets(split_packets, next_link_id,
                                    new KeyCommPathOrLink(key.commodity_id, next_link_id, false),
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
                if(key.isPath){
                    Link next_link = path2outlink.get(key.pathOrlink_id);
                    add_to_lanegroup_packets(split_packets,next_link.getId(),key,vehicle);
                }

                // pathless case
                else {

                    if(is_sink){
                        vehicle.set_next_link_id(id);
                        add_to_lanegroup_packets(split_packets,id ,
                                new KeyCommPathOrLink(key.commodity_id, id, false),
                                vehicle);

                    } else {
                        Long next_link_id = commodity2split.get(key.commodity_id).sample_output_link();
                        vehicle.set_next_link_id(next_link_id);
                        add_to_lanegroup_packets(split_packets,next_link_id ,
                                new KeyCommPathOrLink(key.commodity_id, next_link_id, false),
                                vehicle);

                    }

                }
            }
        }

        return split_packets;
    }

    ////////////////////////////////////////////
    // InterfaceActuatorTarget
    ///////////////////////////////////////////

    @Override
    public void register_actuator(AbstractActuator act) throws OTMException {

        if(act instanceof actuator.ActuatorRampMeter) {
            if(ramp_meter!=null)
                throw new OTMException("Multiple ramp meters assigned to the same link.");
            this.ramp_meter = (actuator.ActuatorRampMeter) act;
        }

        if(act instanceof actuator.ActuatorFD){
            if(actuator_fd!=null)
                throw new OTMException("Multiple fd actiuators assigned to the same link.");
            this.actuator_fd = (actuator.ActuatorFD) act;
        }

    }

    ////////////////////////////////////////////
    // routing getters
    ///////////////////////////////////////////

    // return outlink to split
    public Map<Long,Double> get_splits_for_commodity(Long comm_id){
        if(!commodity2split.containsKey(comm_id))
            return null;
        return commodity2split.get(comm_id).outlink2split;
    }

//    public Link sample_nextlink_for_commodity(Commodity comm){
//
//        // this is a sink, no next link
//        if(is_sink)
//            return null;
//
//        // get next link id
//        Long outlink_id;
//        if(comm.pathfull) {
//            return path2outlink.get(d);
//        }
//
//        // otherwise use split ratios
//        else {
//            outlink_id = commodity2split.get(key.commodity_id).sample_output_link();
//        }
//        return outlink_id;
//    }

    public Collection<Link> get_previous_links(){
        return start_node.in_links.values();
    }

    public Set<RoadConnection> get_roadconnections_leaving(){
        Set<RoadConnection> rcs = new HashSet<>();
        for(AbstractLaneGroup lg : lanegroups_flwdn.values())
            rcs.addAll(lg.outlink2roadconnection.values());
        return rcs;
    }

    public Set<RoadConnection> get_roadconnections_entering(){
        Set<RoadConnection> rcs = new HashSet<>();
        for(Link uplink : start_node.in_links.values())
            if(uplink.outlink2lanegroups.containsKey(getId()))
                for(AbstractLaneGroup lg : uplink.outlink2lanegroups.get(id) )
                    if(lg.outlink2roadconnection.containsKey(getId()))
                        rcs.add(lg.outlink2roadconnection.get(getId()));
        return rcs;
    }

    ////////////////////////////////////////////
    // configuration getters
    ///////////////////////////////////////////

    public int get_num_dn_lanes(){
        return full_lanes +
                (road_geom==null||road_geom.dn_in==null ? 0 : road_geom.dn_in.lanes) +
                (road_geom==null||road_geom.dn_out==null ? 0 : road_geom.dn_out.lanes);
    }

    public int get_num_up_lanes(){
        return full_lanes +
                (road_geom==null||road_geom.up_in==null ? 0 : road_geom.up_in.lanes) +
                (road_geom==null||road_geom.up_out==null ? 0 : road_geom.up_out.lanes);
    }

    // returns length in meters
    public float get_length_for_lane(int lane){

        // TODO REPAIR THIS

        if(lane<1)
            return 0f;

        if(road_geom==null)
            return this.length;

        if(lane<=road_geom.dn_in.lanes)
            return road_geom.dn_in.length;
        else if(lane<=road_geom.dn_in.lanes+full_lanes)
            return this.length;
        else if (lane<=road_geom.dn_in.lanes+full_lanes+road_geom.dn_out.lanes)
            return road_geom.dn_out.length;
        else
            return 0f;
    }

    public Side get_side_for_dn_lane(int lane){
        int in_lanes = road_geom!=null && road_geom.dn_in!=null ? road_geom.dn_in.lanes : 0;
        int out_lanes = road_geom!=null && road_geom.dn_out!=null ? road_geom.dn_out.lanes : 0;

        if(lane<=in_lanes)
            return Side.in;

        if(lane<=in_lanes+full_lanes)
            return Side.stay;

        if(lane<=in_lanes+full_lanes+out_lanes)
            return Side.out;

        return null;
    }

    public Set<AbstractLaneGroup> get_unique_lanegroups_for_dn_lanes(int from_lane, int to_lane) {
        Set<AbstractLaneGroup> x = new HashSet<>();
        for (int lane = from_lane; lane <= to_lane; lane++)
            x.add(get_lanegroup_for_dn_lane(lane));
        return x;
    }

    public Set<AbstractLaneGroup> get_unique_lanegroups_for_up_lanes(int from_lane, int to_lane) {
        Set<AbstractLaneGroup> x = new HashSet<>();
        for (int lane = from_lane; lane <= to_lane; lane++)
            x.add(get_lanegroup_for_up_lane(lane));
        return x;
    }

    public AbstractLaneGroup get_lanegroup_for_dn_lane(int lane){
        return dnlane2lanegroup.get(lane);
    }

    public AbstractLaneGroup get_lanegroup_for_up_lane(int lane){
        AbstractLaneGroup lg = lanegroup_flwside_in !=null ? lanegroup_flwside_in : get_inner_full_lanegroup();
        while(true){
            if(lane<=lg.start_lane_up+lg.num_lanes-1)
                return lg;
            lg = lg.neighbor_up_out !=null ? lg.neighbor_up_out : lg.neighbor_out;
            if(lg==null)
                break;
        }
        return null;
    }

    public AbstractLaneGroup get_inner_full_lanegroup(){
        return dnlane2lanegroup.get( road_geom==null || road_geom.dn_in==null ? 1 : road_geom.dn_in.lanes+1 );
    }

    public AbstractLaneGroup get_outer_full_lanegroup(){
        return dnlane2lanegroup.get( road_geom==null || road_geom.dn_in==null ? full_lanes : road_geom.dn_in.lanes+full_lanes );
    }

    ////////////////////////////////////////////
    // parameter getters
    ///////////////////////////////////////////

    public float get_max_vehicles(){
        return lanegroups_flwdn.values().stream().
                map(x->x.max_vehicles).
                reduce(0f,(i,j)->i+j);
    }

    ////////////////////////////////////////////
    // state and performance getters
    ///////////////////////////////////////////

    public double get_veh() {
        return lanegroups_flwdn.values().stream()
                .mapToDouble(x->x.get_total_vehicles())
                .sum();
    }

    public double get_veh_for_commodity(Long commodity_id) {
        return lanegroups_flwdn.values().stream()
                .mapToDouble(x->x.vehs_dwn_for_comm(commodity_id))
                .sum();
    }

    // return the instantaneous travel time averaged over the lane groups
    // This is used by the path travel timer.
    // It should be improved to measure travel times on the lanegroups used by the path.
    public double get_current_average_travel_time(){
        return lanegroups_flwdn.values().stream().mapToDouble(lg->lg.get_current_travel_time()).average().getAsDouble();
    }

    ////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public ScenarioElementType getScenarioElementType() {
        return ScenarioElementType.link;
    }

    ////////////////////////////////////////////
    // private and other
    ///////////////////////////////////////////

    private void add_to_lanegroup_packets(Map<Long, AbstractPacketLaneGroup> split_packets,Long nextlink_id,KeyCommPathOrLink key,Double vehicles){
        AbstractPacketLaneGroup new_packet;
        if(split_packets.containsKey(nextlink_id)){
            new_packet = split_packets.get(nextlink_id);
        } else {
            new_packet = model.create_lanegroup_packet();
            split_packets.put(nextlink_id,new_packet);
        }
        new_packet.add_fluid(key,vehicles);
    }

    private void add_to_lanegroup_packets(Map<Long, AbstractPacketLaneGroup> split_packets,Long nextlink_id,KeyCommPathOrLink key,AbstractVehicle vehicle){
        AbstractPacketLaneGroup new_packet;
        if(split_packets.containsKey(nextlink_id)){
            new_packet = split_packets.get(nextlink_id);
        } else {
            new_packet = model.create_lanegroup_packet();
            split_packets.put(nextlink_id,new_packet);
        }
        new_packet.add_vehicle(key,vehicle);
    }

    @Override
    public String toString() {
        return String.format("link %d",id);
    }

    public jaxb.Link to_jaxb(){
        jaxb.Link jlink = new jaxb.Link();
        jlink.setId(this.getId());
        jlink.setStartNodeId(this.start_node.getId());
        jlink.setEndNodeId(this.end_node.getId());
        jlink.setLength(this.length);
        jlink.setFullLanes(this.full_lanes);
        if(this.road_geom!=null)
            jlink.setRoadgeom(this.road_geom.id);
        jlink.setRoadparam(this.road_param.getId());
        jlink.setRoadType(this.road_type.toString());
        jaxb.Points jpoints = new jaxb.Points();
        jlink.setPoints(jpoints);
        for(common.Point point : this.shape){
            jaxb.Point jpoint = new jaxb.Point();
            jpoints.getPoint().add(jpoint);
            jpoint.setX(point.x);
            jpoint.setY(point.y);
        }
        return jlink;
    }


}
