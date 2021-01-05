package core;

import actuator.AbstractActuator;
import actuator.ActuatorFlowToLinks;
import actuator.InterfaceActuatorTarget;
import error.OTMErrorLog;
import error.OTMException;
import core.geometry.RoadGeometry;
import jaxb.Points;
import jaxb.Roadparam;
import lanechange.InterfaceLaneSelector;
import lanechange.LinkLaneSelector;
import profiles.SplitMatrixProfile;
import traveltime.LinkTravelTimer;
import core.packet.PacketLaneGroup;
import core.packet.PacketLink;

import java.util.*;

import static java.util.stream.Collectors.toSet;

public class Link implements InterfaceScenarioElement, InterfaceActuatorTarget {


    public enum RoadType {none,offramp,onramp,freeway,connector,bridge,ghost}

    // basics ........................................
    protected final long id;
    protected final Network network;
    protected final float length;          // meters
    protected final int full_lanes;
    protected final core.Node start_node;
    protected final core.Node end_node;
    protected boolean is_source;
    protected boolean is_sink;

    // parameters .........................................
    protected final RoadType road_type;
    protected final RoadGeometry road_geom;
    public final Roadparam road_param_full;        // for the sake of writing again to jaxb
    protected final List<Point> shape;           // not used by otm-sim

    // model .............................................
    protected AbstractModel model;
    protected boolean is_model_source_link;

    // lane selection model
    protected LinkLaneSelector lane_selector;  // comm->lane selector

    // lanegroups ......................................

    // Longitudinal lanegroups: flow exits from the bottom edge.
    // There are stay lanegroups and downstream addlanes
    // ordered from inner to outer
    protected List<AbstractLaneGroup> lgs;

    // barriers
    protected Set<Barrier> in_barriers;
    protected Set<Barrier> out_barriers;

    // downstream lane count -> lane group
    protected Map<Integer, AbstractLaneGroup> dnlane2lanegroup;

    // routing information ...............................

    // map from path id (uses this link) to next link id (exits this link)
    protected Map<Long,Link> path2outlink;

    // outlink -> lanegroups from which outlink is reachable
    protected Map<Long,Set<AbstractLaneGroup>> outlink2lanegroups;

    // splits
    protected Map<Long, SplitMatrixProfile> split_profile; // commodity -> split matrix profile

    // control flows to downstream links
    protected ActuatorFlowToLinks act_flowToLinks;

    // demands ............................................
    protected Set<AbstractDemandGenerator> demandGenerators;

    // travel timer
    public LinkTravelTimer link_tt;

    ///////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public Link(Network network, jaxb.Roadparam rp, long id, float length, int full_lanes, Node start_node, Node end_node, RoadGeometry rg, Link.RoadType rt,Points jpoints) throws OTMException {

        if (start_node == null)
            throw new OTMException("Unknown start node id in link " + id);

        if (end_node == null)
            throw new OTMException("Unknown end node id in link " + id);

        this.id = id;
        this.network = network;
        this.length = length;
        this.full_lanes = full_lanes;
        this.start_node = start_node;
        this.end_node = end_node;
        this.road_param_full = rp;
        this.road_type = (rt == null) ? RoadType.none : rt;
        this.road_geom = rg;
        this.is_source = true;
        this.is_sink = true;

        lgs = new ArrayList<>();
        dnlane2lanegroup = new HashMap<>();
        path2outlink = new HashMap<>();
        outlink2lanegroups = new HashMap<>();
        demandGenerators = new HashSet<>();

        this.start_node.add_output_link(this);
        this.end_node.add_input_link(this);

        // shape
        this.shape = new ArrayList<>();
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
    // InterfaceActuatorTarget
    ///////////////////////////////////////////

    @Override
    public String getTypeAsTarget() {
        return "link";
    }

    @Override
    public long getIdAsTarget() {
        return id;
    }

    @Override
    public void register_actuator(Set<Long> commids, AbstractActuator act,boolean override) throws OTMException {

        if(act instanceof ActuatorFlowToLinks) {
//            if(!override && act_flowToLinks!=null)
//                throw new OTMException("Link already has a flow actuator.");
            this.act_flowToLinks = (ActuatorFlowToLinks) act;
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
        if( lgs ==null )
            errorLog.addError("link " + id + ": lanegroups==null");

        // all lanegroups are represented in dnlane2lanegroup
        if(lgs !=null && dnlane2lanegroup !=null) {
            Set<Long> A = dnlane2lanegroup.values().stream().map(x->x.id).collect(toSet());
            Set<Long> B = lgs.stream().map(x->x.id).collect(toSet());
            if (!A.equals(B))
                errorLog.addError("link " + id + ": not all lanegroups are represented in dnlane2lanegroup");
        }

        // lanegroups
        if(lgs !=null)
            lgs.forEach(x->x.validate(errorLog));

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
        for(AbstractLaneGroup lg : lgs)
            lg.initialize(scenario,start_time);

        if(split_profile!=null)
            for(SplitMatrixProfile x : split_profile.values())
                x.initialize(scenario.dispatcher);

        if(demandGenerators !=null)
            for(AbstractDemandGenerator gen : demandGenerators)
                gen.initialize(scenario);

        if(lane_selector!=null)
            lane_selector.initialize(scenario,start_time);
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

    public void set_lanegroups(List<AbstractLaneGroup> lgs) {
        this.lgs = lgs;
        dnlane2lanegroup = new HashMap<>();
        for (AbstractLaneGroup lg : lgs)
            for (int lane=lg.start_lane_dn;lane<lg.start_lane_dn+lg.num_lanes;lane++)                       // iterate through dn lanes
                dnlane2lanegroup.put(lane, lg);
    }

    public double get_max_vehicles(){
        return lgs.stream().mapToDouble(x->x.get_max_vehicles()).sum();
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
        return lgs.stream()
                .flatMap(lg->lg.outlink2roadconnection.values().stream())
                .collect(toSet());
    }

    public Set<RoadConnection> get_roadconnections_entering(){
        Set<RoadConnection> rcs = start_node.in_links.values().stream()
                .flatMap(lk->lk.lgs.stream())
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
    // state and performance getters
    ///////////////////////////////////////////

    public double get_veh() {
        return lgs.stream()
                .mapToDouble(x->x.get_total_vehicles())
                .sum();
    }

    public double get_veh_for_commodity(Long commodity_id) {
        return lgs.stream()
                .mapToDouble(x->x.get_total_vehicles_for_commodity(commodity_id))
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

    ////////////////////////////////////////////
    // API
    ///////////////////////////////////////////

    public Scenario get_scenario(){
        return network.scenario;
    }

    public Network get_network(){
        return network;
    }

    public int get_full_lanes(){
        return full_lanes;
    }

    public float get_full_length(){
        return length;
    }

    public Node get_start_node(){
        return start_node;
    }

    public Node get_end_node(){
        return end_node;
    }

    public boolean is_model_source_link(){
        return is_model_source_link;
    }

    public AbstractModel get_model(){
        return model;
    }

    public boolean is_source(){
        return is_source;
    }

    public boolean is_sink(){
        return is_sink;
    }

    public int get_num_dn_in_lanes(){
        if(road_geom==null)
            return 0;
        if(road_geom.in !=null)
            return road_geom.in.lanes;
        return 0;
    }

    public int get_num_dn_out_lanes(){
        if(road_geom==null)
            return 0;
        if(road_geom.out !=null)
            return road_geom.out.lanes;
        return 0;
    }

    public int get_num_up_in_lanes(){
        if(road_geom==null)
            return 0;
        if(road_geom.in !=null && road_geom.in.isfull)
            return road_geom.in.lanes;
        return 0;
    }

    public int get_num_up_out_lanes(){
        if(road_geom==null)
            return 0;
        if(road_geom.out !=null && road_geom.out.isfull)
            return road_geom.out.lanes;
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
        int in_lanes = road_geom!=null && road_geom.in !=null ? road_geom.in.lanes : 0;
        int out_lanes = road_geom!=null && road_geom.out !=null ? road_geom.out.lanes : 0;

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
        if(road_geom==null || road_geom.in ==null || road_geom.in.isfull ){
            return dnlane2lanegroup.get(1);
        }
        else {
            return dnlane2lanegroup.get(road_geom.in.lanes + 1);
        }
    }

    public AbstractLaneGroup get_outer_full_lanegroup(){
        return dnlane2lanegroup.get(
                road_geom==null || road_geom.in ==null ?
                        full_lanes :
                        road_geom.in.lanes+full_lanes );

    }

    public RoadType get_road_type(){
        return road_type;
    }

    public Map<State, InterfaceLaneSelector> get_lane_selector_for_lane_group(long lgid){
        return lane_selector.get_laneselector_for_lanegroup(lgid);
    }

    public List<AbstractLaneGroup> get_lgs(){
        return lgs;
    }

    public Set<Barrier> get_in_barriers(){
        return in_barriers;
    }

    public Set<Barrier> get_out_barriers(){
        return out_barriers;
    }

    public Link get_next_link_in_path(long pathid){
        return path2outlink.get(pathid);
    }

    public Set<AbstractLaneGroup> get_lanegroups_for_outlink(long linkid){
        return outlink2lanegroups.get(linkid);
    }

    public Collection<Long> get_outlink_ids(){
        return outlink2lanegroups.keySet();
    }

    public boolean has_demands(){
        return demandGenerators !=null && !demandGenerators.isEmpty();
    }

    public Set<AbstractDemandGenerator> get_demandGenerators(){
        return demandGenerators;
    }

}
