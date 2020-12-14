package core;

import actuator.ActuatorFlowToLinks;
import error.OTMErrorLog;
import error.OTMException;
import core.geometry.RoadGeometry;
import jaxb.Commodity;
import jaxb.Points;
import jaxb.Roadparam;
import models.AbstractModel;
import profiles.SplitMatrixProfile;
import traveltime.LinkTravelTimer;
import core.packet.PacketLaneGroup;
import core.packet.PacketLink;
import utils.OTMUtils;

import java.util.*;

import static java.util.stream.Collectors.toSet;

public class Link implements InterfaceScenarioElement {

    public enum RoadType {none,offramp,onramp,freeway,connector,bridge,ghost}

    // basics ........................................
    protected final long id;
    public Network network;
    public final float length;          // meters
    public final int full_lanes;
    public core.Node start_node;
    public core.Node end_node;
    public boolean is_source;
    public boolean is_sink;

    // parameters .........................................
    public RoadType road_type;
    public RoadGeometry road_geom;
    public Roadparam road_param_full;        // for the sake of writing again to jaxb
    public List<Point> shape;           // not used by otm-sim

    // model .............................................
    public AbstractModel model;
    public boolean is_model_source_link;

    // lanegroups ......................................

    // Longitudinal lanegroups: flow exits from the bottom edge.
    // There are stay lanegroups and downstream addlanes
    // ordered from inner to outer
    public List<AbstractLaneGroup> lanegroups_flwdn;

    // barriers
    public Set<Barrier> in_barriers;
    public Set<Barrier> out_barriers;

    // downstream lane count -> lane group
    public Map<Integer, AbstractLaneGroup> dnlane2lanegroup;

    // routing information ...............................

    // map from path id (uses this link) to next link id (exits this link)
    public Map<Long,Link> path2outlink;

    // outlink -> lanegroups from which outlink is reachable
    public Map<Long,Set<AbstractLaneGroup>> outlink2lanegroups;

    // splits
    protected Map<Long, SplitMatrixProfile> split_profile; // commodity -> split matrix profile

    // control flows to downstream links
    public ActuatorFlowToLinks act_flowToLinks;

    // demands ............................................
    public Set<AbstractDemandGenerator> demandGenerators;

    // travel timer
    public LinkTravelTimer link_tt;

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
        this.road_param_full = rp;

        // shape
        this.shape = new ArrayList<>();

        // lanegroups ......................................
        lanegroups_flwdn = new ArrayList<>();
        dnlane2lanegroup = new HashMap<>();

        // routing ............................................
        path2outlink = new HashMap<>();
        outlink2lanegroups = new HashMap<>();

        // demands ............................................
        demandGenerators = new HashSet<>();

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

    public void allocate_splits(Set<Long> pathless_comms){
        split_profile = new HashMap<>();
        for(Long c : pathless_comms) {
            split_profile.put(c, new SplitMatrixProfile(c,this));
        }
    }

    ///////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////

    @Override
    public final Long getId() {
        return id;
    }

    @Override
    public final ScenarioElementType getSEType() {
        return ScenarioElementType.link;
    }

    @Override
    public void validate(OTMErrorLog errorLog) {

        if( length<=0 )
            errorLog.addError("link " + id + ": length<=0");
        if( start_node==null )
            errorLog.addError("link " + id + ": start_node==null");
        if( end_node==null )
            errorLog.addError("link " + id + ": end_node==null");
        if( model ==null )
            errorLog.addError("link " + id + ": model==null");
        if( dnlane2lanegroup ==null )
            errorLog.addError("link " + id + ": dnlane2lanegroup==null");
        if( lanegroups_flwdn ==null )
            errorLog.addError("link " + id + ": lanegroups==null");

        // all lanegroups are represented in dnlane2lanegroup
        if(lanegroups_flwdn !=null && dnlane2lanegroup !=null) {
            Set<Long> A = dnlane2lanegroup.values().stream().map(x->x.id).collect(toSet());
            Set<Long> B = lanegroups_flwdn.stream().map(x->x.id).collect(toSet());
            if (!A.equals(B))
                errorLog.addError("link " + id + ": not all lanegroups are represented in dnlane2lanegroup");
        }

        // lanegroups
        if(lanegroups_flwdn !=null)
            lanegroups_flwdn.forEach(x->x.validate(errorLog));

        // out_road_connections all lead to links that are immediately downstream
        Set<Link> dwn_links = this.get_roadconnections_leaving().stream()
                .map(rc->rc.end_link)
                .collect(toSet());
        if(!end_node.out_links.containsAll(dwn_links))
            errorLog.addError("some outlinks are not immediately downstream");

        // I have a split_profile if there is a downstream bifurction
//        if(end_node.out_links.size()>1){
//
//            Set<Long> pathlesscomms = network.scenario.commodities.values().stream()
//                    .filter(c->!c.pathfull)
//                    .map(x->x.getId())
//                    .collect(toSet());
//            if(!split_profile.keySet().containsAll(pathlesscomms)){
//                pathlesscomms.removeAll(split_profile.keySet());
//                errorLog.addError(String.format("Node %d lacks splits for incoming link %d, commodities ",
//                        end_node.getId(),id, OTMUtils.comma_format(pathlesscomms)));
//            }
//
//        }

        if(split_profile!=null)
            split_profile.values().stream().forEach(x -> x.validate(network.scenario,errorLog));

        if(this.demandGenerators !=null)
            demandGenerators.stream().forEach(x->x.profile.validate(errorLog));
    }

    public void initialize(Scenario scenario, float start_time) throws OTMException {
        for(AbstractLaneGroup lg : lanegroups_flwdn)
            lg.initialize(scenario,start_time);

        if(split_profile!=null)
            for(SplitMatrixProfile x : split_profile.values())
                x.initialize(scenario.dispatcher);

        if(demandGenerators !=null)
            for(AbstractDemandGenerator gen : demandGenerators)
                gen.initialize(scenario);
    }

    @Override
    public jaxb.Link to_jaxb(){
        jaxb.Link jlink = new jaxb.Link();
        jlink.setId(this.getId());
        jlink.setStartNodeId(this.start_node.getId());
        jlink.setEndNodeId(this.end_node.getId());
        jlink.setLength(this.length);
        jlink.setFullLanes(this.full_lanes);
        if(this.road_geom!=null)
            jlink.setRoadgeom(this.road_geom.id);
        jlink.setRoadparam(this.road_param_full.getId());
        if(!road_type.equals(RoadType.none))
            jlink.setRoadType(this.road_type.toString());
        jaxb.Points jpoints = new jaxb.Points();
        jlink.setPoints(jpoints);
        for(core.Point point : this.shape){
            jaxb.Point jpoint = new jaxb.Point();
            jpoints.getPoint().add(jpoint);
            jpoint.setX(point.x);
            jpoint.setY(point.y);
        }
        return jlink;
    }

    ///////////////////////////////////////////

    public void delete(){
        network = null;
        start_node = null;
        end_node = null;
        if(lanegroups_flwdn !=null)
            lanegroups_flwdn.forEach(lg->lg.delete());
        lanegroups_flwdn = null;
        dnlane2lanegroup = null;
        path2outlink = null;
        outlink2lanegroups = null;
        split_profile = null;
        if(demandGenerators !=null)
            demandGenerators.forEach(s->s.delete());
        demandGenerators = null;
        road_type = null;
        road_geom = null;
        model = null;
//        travel_timers = null;
        shape = null;
    }

    public void set_flwdn_lanegroups(List<AbstractLaneGroup> lgs) {

        lanegroups_flwdn = new ArrayList<>();
        dnlane2lanegroup = new HashMap<>();

        if(lgs==null || lgs.isEmpty())
            return;

        // lanegroups
        for(AbstractLaneGroup lg : lgs)
            lanegroups_flwdn.add(lg);

        // dnlane2lanegroup
        for (AbstractLaneGroup lg : lgs)
            for (int lane=lg.start_lane_dn;lane<lg.start_lane_dn+lg.num_lanes;lane++)                       // iterate through dn lanes
                dnlane2lanegroup.put(lane, lg);
    }

    public void set_model(AbstractModel newmodel, boolean is_model_source_link) throws OTMException {

        if (model==null){
            this.model = newmodel;
            this.is_model_source_link = is_model_source_link;
            return;
        }

        if(model.is_default && !newmodel.is_default) {
            this.model = newmodel;
            this.is_model_source_link = is_model_source_link;
            return;
        }

        if(model.is_default && newmodel.is_default)
            throw new OTMException("Multiple default models");

        if(!model.is_default && !newmodel.is_default)
            throw new OTMException("ModelType multiply assigned for link " + this.id);
    }

    public double get_max_vehicles(){
        return lanegroups_flwdn.stream().mapToDouble(x->x.get_max_vehicles()).sum();
    }

    public void set_actuator_flowToLinks(ActuatorFlowToLinks act){
        this.act_flowToLinks = act;
    }

    ////////////////////////////////////////////
    // inter-link dynamics
    ///////////////////////////////////////////

    // split a core.packet according to next links.
    // for pathfull commodities, the next link is the next in the path.
    // For pathless, it is sampled from the split ratios
    // this assigns the state for the split core.packet but does not yet assign
    // the target road connection. This is done after joining the lane group.
    // This DOES NOT update the keys within the PacketLaneGroup's
    // and hence they are out of sync with the next link ids (keys in the map)
    public Map<Long, PacketLaneGroup> split_packet(PacketLink vp){

        // initialize lanegroup_packets (next link id -> core.packet)
        Map<Long, PacketLaneGroup> split_packets = new HashMap<>();

        boolean has_macro = !vp.no_macro();
        boolean has_micro = !vp.no_micro();

        // process the macro state
        if(has_macro) {

            // do scaling calcultions that depend on the core.packet
            if(act_flowToLinks!=null && vp.road_connection==act_flowToLinks.rc)
                act_flowToLinks.update_for_packet(vp);

            for (Map.Entry<State, Double> e : vp.state2vehicles.entrySet()) {

                State state = e.getKey();
                Double vehicles = e.getValue();

                if(vehicles==0d)
                    continue;

                // pathfull
                if (state.isPath) {
                    Link next_link = path2outlink.get(state.pathOrlink_id);
                    add_to_lanegroup_packets(split_packets,next_link==null?null:next_link.getId(),state,vehicles);
                }

                // pathless
                else {

                    if( is_sink ){
                        add_to_lanegroup_packets(split_packets, id,
                                new State(state.commodity_id, id, false),
                                vehicles );
                    }

                    else if( this.outlink2lanegroups.size()==1){
                        Long next_link_id = outlink2lanegroups.keySet().iterator().next();
                        add_to_lanegroup_packets(split_packets, next_link_id,
                                new State(state.commodity_id, next_link_id, false),
                                vehicles );
                    }

                    else {

                        Map<Long, Double> current_splits = split_profile.get(state.commodity_id).outlink2split;

                        // calculate sumbetac
                        double sumbetac = act_flowToLinks!=null && vp.road_connection==act_flowToLinks.rc ? act_flowToLinks.calculate_sumbetac(current_splits) :  0d;

                        for ( Map.Entry<Long, Double> e2 : current_splits.entrySet() ) {
                            Long next_link_id = e2.getKey();
                            double split = e2.getValue();

                            // get split for offramp
                            if(act_flowToLinks!=null && vp.road_connection==act_flowToLinks.rc ){

                                if( act_flowToLinks.outlink_ids.contains(next_link_id) )
                                    split =  act_flowToLinks.outlink2portion.get(next_link_id);
                                else {
                                    if (sumbetac > 0)
                                        split *= act_flowToLinks.gamma / sumbetac;
                                    else
                                        split = 1d / (end_node.out_links.size() - act_flowToLinks.outlink_ids.size());
                                }
                            }

                            if(split>0d){
                                add_to_lanegroup_packets(split_packets, next_link_id,
                                        new State(state.commodity_id, next_link_id, false),
                                        vehicles * split);
                            }
                        }
                    }
                }
            }
        }

        // process the micro state
        if(has_micro){
            for(AbstractVehicle vehicle : vp.vehicles){

                State key = vehicle.get_state();

                // pathfull case
                if(key.isPath){
                    if(is_sink){
                        add_to_lanegroup_packets(split_packets,null,key,vehicle);
                    }
                    else if(path2outlink.containsKey(key.pathOrlink_id)){
                        Link next_link = path2outlink.get(key.pathOrlink_id);
                        add_to_lanegroup_packets(split_packets,next_link.getId(),key,vehicle);
                    }
                }

                // pathless case
                else {

                    if(is_sink){
                        vehicle.set_next_link_id(id);
                        add_to_lanegroup_packets(split_packets,id ,
                                new State(key.commodity_id, id, false),
                                vehicle);

                    } else {
                        Long next_link_id = sample_next_link(key.commodity_id);
                        vehicle.set_next_link_id(next_link_id);
                        add_to_lanegroup_packets(split_packets,next_link_id ,
                                new State(key.commodity_id, next_link_id, false),
                                vehicle);

                    }

                }
            }
        }

        return split_packets;
    }

    ////////////////////////////////////////////
    // routing getters
    ///////////////////////////////////////////

    public Long sample_next_link(Long comm_id){
        if(split_profile!=null)
            return split_profile.get(comm_id).sample_output_link();
        else
            return end_node.out_links.iterator().next().getId();
    }

    // return outlink to split
    public Map<Long,Double> get_splits_for_commodity(Long comm_id){
        if(!split_profile.containsKey(comm_id))
            return null;
        return split_profile.get(comm_id).outlink2split;
    }

    public Collection<Link> get_previous_links(){
        return start_node.in_links.values();
    }

    public Set<RoadConnection> get_roadconnections_leaving(){
        return lanegroups_flwdn.stream()
                .flatMap(lg->lg.outlink2roadconnection.values().stream())
                .collect(toSet());
    }

    public Set<RoadConnection> get_roadconnections_entering(){
        Set<RoadConnection> rcs = start_node.in_links.values().stream()
                .flatMap(lk->lk.lanegroups_flwdn.stream())
                .flatMap(lg->lg.outlink2roadconnection.values().stream())
                .filter(rc->rc.end_link.getId()==this.getId())
                .collect(toSet());
        return rcs;
    }

    public SplitMatrixProfile get_split_profile(long commid){
        return split_profile.get(commid);
    }

    public void remove_split_profile(long commid){
        split_profile.remove(commid);
    }

    public boolean have_split_for_commodity(long commid){
        return split_profile.containsKey(commid);
    }

    public void set_split_profile(long commid, SplitMatrixProfile smp){
        split_profile.put(commid, smp);
    }

    ////////////////////////////////////////////
    // configuration getters
    ///////////////////////////////////////////

    public int get_num_full_lanes(){
        return full_lanes;
    }

    public int get_num_dn_in_lanes(){
        if(road_geom==null)
            return 0;
        if(road_geom.dn_in!=null)
            return road_geom.dn_in.lanes;
        return 0;
    }

    public int get_num_dn_out_lanes(){
        if(road_geom==null)
            return 0;
        if(road_geom.dn_out!=null)
            return road_geom.dn_out.lanes;
        return 0;
    }

    public int get_num_up_in_lanes(){
        if(road_geom==null)
            return 0;
        if(road_geom.dn_in!=null && road_geom.dn_in.isfull)
            return road_geom.dn_in.lanes;
        return 0;
    }

    public int get_num_up_out_lanes(){
        if(road_geom==null)
            return 0;
        if(road_geom.dn_out!=null && road_geom.dn_out.isfull)
            return road_geom.dn_out.lanes;
        return 0;
    }

    public int get_num_dn_lanes(){
        return full_lanes + get_num_dn_in_lanes() + get_num_dn_out_lanes();
    }

    public int get_num_up_lanes(){
        return full_lanes + get_num_up_in_lanes() + get_num_up_out_lanes();
    }

    // whether this lane belongs to the inside addlane, the full lanes or the outside addlane.
    public core.geometry.Side get_side_for_dn_lane(int lane){
        int in_lanes = road_geom!=null && road_geom.dn_in!=null ? road_geom.dn_in.lanes : 0;
        int out_lanes = road_geom!=null && road_geom.dn_out!=null ? road_geom.dn_out.lanes : 0;

        if(lane<=in_lanes)
            return core.geometry.Side.in;

        if(lane<=in_lanes+full_lanes)
            return core.geometry.Side.middle;

        if(lane<=in_lanes+full_lanes+out_lanes)
            return core.geometry.Side.out;

        return null;
    }

    public Set<AbstractLaneGroup> get_unique_lanegroups_for_dn_lanes(int from_lane, int to_lane) {
        Set<AbstractLaneGroup> x = new HashSet<>();
        for (int lane = from_lane; lane <= to_lane; lane++)
            x.add(get_lanegroup_for_dn_lane(lane));
        return x;
    }

    public AbstractLaneGroup get_lanegroup_for_dn_lane(int lane){
        return dnlane2lanegroup.get(lane);
    }

    public AbstractLaneGroup get_lanegroup_for_up_lane(int lane){
        AbstractLaneGroup lg = get_inner_full_lanegroup();
        while(true){
            if(lane<=lg.start_lane_up+lg.num_lanes-1)
                return lg;
            lg = lg.neighbor_out;
            if(lg==null)
                break;
        }
        return null;
    }

    public AbstractLaneGroup get_inner_full_lanegroup(){
        if(road_geom==null || road_geom.dn_in==null || road_geom.dn_in.isfull ){
            return dnlane2lanegroup.get(1);
        }
        else {
            return dnlane2lanegroup.get(road_geom.dn_in.lanes + 1);
        }
    }

    public AbstractLaneGroup get_outer_full_lanegroup(){
        return dnlane2lanegroup.get(
                road_geom==null || road_geom.dn_in==null ?
                full_lanes :
                road_geom.dn_in.lanes+full_lanes );

    }

    ////////////////////////////////////////////
    // state and performance getters
    ///////////////////////////////////////////

    public double get_veh() {
        return lanegroups_flwdn.stream()
                .mapToDouble(x->x.get_total_vehicles())
                .sum();
    }

    public double get_veh_for_commodity(Long commodity_id) {
        return lanegroups_flwdn.stream()
                .mapToDouble(x->x.vehs_dwn_for_comm(commodity_id))
                .sum();
    }

    ////////////////////////////////////////////
    // private
    ///////////////////////////////////////////

    private void add_to_lanegroup_packets(Map<Long, PacketLaneGroup> split_packets, Long nextlink_id, State key, Double vehicles){
        PacketLaneGroup new_packet;
        if(split_packets.containsKey(nextlink_id)){
            new_packet = split_packets.get(nextlink_id);
        } else {
            new_packet = model.create_lanegroup_packet();
            split_packets.put(nextlink_id,new_packet);
        }
        new_packet.add_fluid(key,vehicles);
    }

    private void add_to_lanegroup_packets(Map<Long, PacketLaneGroup> split_packets, Long nextlink_id, State key, AbstractVehicle vehicle){
        PacketLaneGroup new_packet;
        if(split_packets.containsKey(nextlink_id)){
            new_packet = split_packets.get(nextlink_id);
        } else {
            new_packet = model.create_lanegroup_packet();
            split_packets.put(nextlink_id,new_packet);
        }
        new_packet.add_vehicle(key,vehicle);
    }

    ///////////////////////////////////////
    // toString
    ///////////////////////////////////////

    @Override
    public String toString() {
        return String.format("link %d",id);
    }

}
