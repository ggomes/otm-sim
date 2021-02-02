package core;

import actuator.AbstractActuator;
import actuator.ActuatorFlowToLinks;
import actuator.InterfaceTarget;
import error.OTMErrorLog;
import error.OTMException;
import core.geometry.RoadGeometry;
import jaxb.Points;
import jaxb.Roadparam;
import lanechange.AbstractLaneSelector;
import profiles.SplitMatrixProfile;
import traveltime.LinkTravelTimer;
import core.packet.PacketLaneGroup;
import core.packet.PacketLink;

import java.util.*;

import static java.util.stream.Collectors.toSet;

/** A segment of road in the network.
 *
 */
public class Link implements InterfaceScenarioElement, InterfaceTarget {

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
    public Long alt_next_link;

    // parameters .........................................
    protected final RoadType road_type;
    protected final RoadGeometry road_geom;
    public final Roadparam road_param_full;        // for the sake of writing again to jaxb
    protected final List<Point> shape;           // not used by otm-sim

    // model .............................................
    protected AbstractModel model;
    protected boolean is_model_source_link;

    // set of keys for states in this link
    public Set<State> states;

    // lane selection model
    protected AbstractLaneSelector lane_selector;

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
    public Set<ActuatorFlowToLinks> unique_acts_flowToLinks;
    public Map<Long,Map<Long,ActuatorFlowToLinks>> acts_flowToLinks; // road connection->commodity->actuator

    // demands ............................................
    protected Set<AbstractDemandGenerator> demandGenerators;

    // travel timer
    public LinkTravelTimer link_tt;

    ///////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public Link(Network network, jaxb.Roadparam rp, long id, float length, int full_lanes, Node start_node, Node end_node, RoadGeometry rg, Link.RoadType rt,Points jpoints, Long alt_next_link) throws OTMException {

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
        this.states = new HashSet<>();
        this.alt_next_link = alt_next_link;

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

    public void add_state(long comm_id, Long path_id,Long next_link_id, boolean ispathfull){

        State state = ispathfull ?
            new State(comm_id, path_id, true) :
            new State(comm_id, next_link_id, false);

        states.add(state);

        for (AbstractLaneGroup lg : lgs)
            lg.add_stateXX(state,next_link_id);
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

            if(acts_flowToLinks==null) {
                unique_acts_flowToLinks = new HashSet<>();
                acts_flowToLinks = new HashMap<>();
            }

            if(commids.size()!=1)
                throw new OTMException("-28904jgq-ie");

            ActuatorFlowToLinks actf2l = (ActuatorFlowToLinks) act;

            unique_acts_flowToLinks.add(actf2l);

            long commid = commids.iterator().next();

            if(split_profile!=null && split_profile.containsKey(commid))
                actf2l.update_splits(split_profile.get(commid).outlink2split);

            for(Long rcid : actf2l.rcids){

                Map<Long,ActuatorFlowToLinks> comm2act;

                if(!acts_flowToLinks.containsKey(rcid)) {
                    comm2act = new HashMap<>();
                    acts_flowToLinks.put(rcid,comm2act);
                }
                else
                    comm2act = acts_flowToLinks.get(rcid);

                if(comm2act.containsKey(commid))
                    throw new OTMException("This link already has a flowtolinks actuator for this commodity");

                comm2act.put(commid,actf2l);
            }



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

    public void validate_pre_init(OTMErrorLog errorLog) {

        if( length<=0 )
            errorLog.addError("link " + id + ": length<=0");
        if( start_node==null )
            errorLog.addError("link " + id + ": start_node==null");
        if( end_node==null )
            errorLog.addError("link " + id + ": end_node==null");
        if( dnlane2lanegroup ==null )
            errorLog.addError("link " + id + ": dnlane2lanegroup==null");
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
            split_profile.values().stream().forEach(x -> x.validate_pre_init(network.scenario,errorLog));

        if(this.demandGenerators !=null)
            demandGenerators.stream().forEach(x->x.profile.validate_pre_init(errorLog));
    }

    public void validate_post_init(OTMErrorLog errorLog) {

        if( model ==null )
            errorLog.addError("link " + id + ": model==null");
        if( lgs ==null )
            errorLog.addError("link " + id + ": lanegroups==null");

        // all lanegroups are represented in dnlane2lanegroup
        if(lgs!=null && dnlane2lanegroup !=null) {
            Set<Long> A = dnlane2lanegroup.values().stream().map(x->x.id).collect(toSet());
            Set<Long> B = lgs.stream().map(x->x.id).collect(toSet());
            if (!A.equals(B))
                errorLog.addError("link " + id + ": not all lanegroups are represented in dnlane2lanegroup");
        }

        // lanegroups
        if(lgs !=null)
            lgs.forEach(x->x.validate_post_init(errorLog));

    }

    public void initialize(Scenario scenario, float start_time) throws OTMException {
        for(AbstractLaneGroup lg : lgs)
            lg.initialize(scenario,start_time);

        // links_without_splits_or_actuators
        Set<Long> pathless_comms = states.stream().filter(s->!s.isPath).map(s->s.commodity_id).collect(toSet());
        for(Long commid : pathless_comms){
            Set<Long> outlinks = new HashSet<>();
            outlinks.addAll(this.outlink2lanegroups.keySet());
            if(split_profile!=null && split_profile.containsKey(commid)) {
                SplitMatrixProfile smp = split_profile.get(commid);
                if(smp.get_splits()!=null)
                    outlinks.removeAll(smp.get_splits().values.keySet());
            }
        }

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

    public void set_lanegroups(List<AbstractLaneGroup> newlgs) {

        // delete existing lane groups (this is to cause obvious problems if there are
        // lingering reference to stale lane groups
        if(this.lgs!=null && !this.lgs.isEmpty())
            for (AbstractLaneGroup lg : this.lgs)
                lg.delete();

        this.lgs = newlgs;
        dnlane2lanegroup = new HashMap<>();
        for (AbstractLaneGroup lg : this.lgs)
            for (int lane=lg.start_lane_dn;lane<lg.start_lane_dn+lg.num_lanes;lane++)                       // iterate through dn lanes
                dnlane2lanegroup.put(lane, lg);
    }

    public double get_max_vehicles(){
        return lgs.stream().mapToDouble(x->x.get_max_vehicles()).sum();
    }


    ////////////////////////////////////////////
    // inter-link dynamics
    ///////////////////////////////////////////

    // split a packet according to next links.
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

            // get comm->Actuator map for this road connection.
            Map<Long,ActuatorFlowToLinks> act_flowToLinks = null;
            if(acts_flowToLinks!=null && acts_flowToLinks.containsKey(vp.road_connection.id))
                act_flowToLinks = acts_flowToLinks.get(vp.road_connection.id);

            // iterate through states in the packet.
            // there should be only one state per pathless commodity, since all have same next_link==this.
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

                        long commid = state.commodity_id;

                        // I have an actuator for this rc and commodity
                        // use the split matrix stored in the actuator.
                        // this split matrix may leave some outlinks undefined. send the remainder to those
                        boolean use_actuator = act_flowToLinks!=null && act_flowToLinks.containsKey(commid);
                        ActuatorFlowToLinks actflowtolinks = use_actuator ? act_flowToLinks.get(commid) : null;
                        if( actflowtolinks!=null && actflowtolinks.initialized) {

                            // remove split flows
                            double remainder = vehicles*(1d - actflowtolinks.total_unactuated_split);
                            double remainder_per_link = 0d;

                            // case I don't have enough vehicles to satisfy controlled flows
                            double prop_factor = 1d;
                            if(remainder<actflowtolinks.remain_total_outlink2flows){
                                prop_factor = remainder/actflowtolinks.remain_total_outlink2flows;
                                remainder_per_link = 0d;
                            }

                            // case I do, I leave a remainder to be distributed amongst unactuated_links_without_splits
                            else if(!actflowtolinks.unactuated_links_without_splits.isEmpty()) {
                                remainder -= actflowtolinks.remain_total_outlink2flows;
                                remainder_per_link = remainder>0 ? remainder / actflowtolinks.unactuated_links_without_splits.size() : 0d;
                            }

                            // iterate over outlinks
                            for(Long next_link_id : outlink2lanegroups.keySet()){
                                double vehicles_to_link;

                                // this link has controlled flow
                                if(actflowtolinks.remain_outlink2flows.containsKey(next_link_id)) {
                                    double actflow = actflowtolinks.remain_outlink2flows.get(next_link_id);
                                    vehicles_to_link = prop_factor * actflow;
                                    actflowtolinks.remain_outlink2flows.put(next_link_id,actflow-vehicles_to_link);
                                    actflowtolinks.remain_total_outlink2flows -= vehicles_to_link;
                                }

                                // this link has splits
                                else if( actflowtolinks.unactuated_splits!=null && actflowtolinks.unactuated_splits.containsKey(next_link_id) )
                                    vehicles_to_link = actflowtolinks.unactuated_splits.get(next_link_id) * vehicles;

                                // this link gets the remainder
                                else
                                    vehicles_to_link = remainder_per_link;

                                // get vehicles going to next link
                                if(vehicles_to_link>0d)
                                    add_to_lanegroup_packets(split_packets, next_link_id,
                                            new State(commid, next_link_id, false),
                                            vehicles_to_link);

                            }

                        }

                        // if I have no actuator for this rc and comm, then we look at the splits in the split_profile.
                        // If splits don't add up to 1, the distribute the remainder evenly among outlinks with no split
                        // defined.
                        else{

                            // case I have no actuator but I have a split ratio matrix
                            if(split_profile!=null && split_profile.containsKey(commid)) {

                                SplitMatrixProfile smp = split_profile.get(commid);
                                if(smp.outlink2split!=null){

                                    // iterate over outlinks
                                    for (Map.Entry<Long,Double> e1 : smp.outlink2split.entrySet()) {
                                        long next_link_id = e1.getKey();
                                        double split = e1.getValue();
                                        add_to_lanegroup_packets(split_packets, next_link_id,
                                                new State(commid, next_link_id, false),
                                                split * vehicles);
                                    }

                                    // remainder for links without splits
                                    if (!smp.outlinks_without_splits.isEmpty()) {
                                        double remainder_per_link = vehicles*(1-smp.total_split)/smp.outlinks_without_splits.size();
                                        for (Long next_link_id : smp.outlinks_without_splits)
                                            add_to_lanegroup_packets(split_packets, next_link_id,
                                                    new State(commid, next_link_id, false),
                                                    remainder_per_link);
                                    }
                                }

                            }

                            // case I have no actuator and no split ratio matrix
                            // flow is distributed evenly among all downstream links
                            else if(!outlink2lanegroups.isEmpty()){
                                double per_link = vehicles/outlink2lanegroups.size();
                                for (Long next_link_id : outlink2lanegroups.keySet())
                                    add_to_lanegroup_packets(split_packets, next_link_id,
                                            new State(commid, next_link_id, false),
                                            per_link);
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

    @Override
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

//    public Map<State, InterfaceLaneSelector> get_lane_selector_for_lane_group(long lgid){
//        return lane_selector.get_laneselector_for_lanegroup(lgid);
//    }

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



//    /** Set the number of vehicles in a link
//     * This only works for a single commodity scenarios, and single lane group links.
//     * @param link_id
//     * @param numvehs_waiting
//     * @param numvehs_transit
//     */
//    public void set_link_vehicles(long link_id, int numvehs_waiting,int numvehs_transit) throws Exception {
//
//        if(link.get_lgs().size()>1)
//            throw new Exception("Cannot call set_link_vehicles on multi-lane group links");
//
////        if(link.model.type!= ModelType.VehicleMeso)
////            throw new Exception("Cannot call set_link_vehicles on non-meso models");
//
//        long comm_id = commodities.keySet().iterator().next();
//        MesoLaneGroup lg = (MesoLaneGroup) link.get_lgs().iterator().next();
//        SplitMatrixProfile smp = lg.get_link().get_split_profile(comm_id);
//
//        // transit queue ................
//        models.vehicle.spatialq.Queue tq = lg.transit_queue;
//        tq.clear();
//        for(int i=0;i<numvehs_transit;i++) {
//            MesoVehicle vehicle = new MesoVehicle(comm_id, null);
//
//            // sample the split ratio to decide where the vehicle will go
//            Long next_link_id = smp.sample_output_link();
//            vehicle.set_next_link_id(next_link_id);
//
//            // set the vehicle's lane group and state
//            vehicle.lg = lg;
//            vehicle.my_queue = tq;
//
//            // add to lane group (as in lg.add_vehicle_packet)
//            tq.add_vehicle(vehicle);
//
//            // register_with_dispatcher dispatch to go to waiting queue
//            Dispatcher dispatcher = scenario.dispatcher;
//            float timestamp = scenario.get_current_time();
//            float transit_time_sec = (float) OTMUtils.random_zero_to_one()*lg.transit_time_sec;
//            dispatcher.register_event(new EventTransitToWaiting(dispatcher,timestamp + transit_time_sec,vehicle));
//        }
//
//        // waiting queue .................
//        models.vehicle.spatialq.Queue wq = lg.waiting_queue;
//        wq.clear();
//        for(int i=0;i<numvehs_waiting;i++) {
//            MesoVehicle vehicle = new MesoVehicle(comm_id, null);
//
//            // sample the split ratio to decide where the vehicle will go
//            Long next_link_id = smp.sample_output_link();
//            vehicle.set_next_link_id(next_link_id);
//
//            // set the vehicle's lane group and key
//            vehicle.lg = lg;
//            vehicle.my_queue = wq;
//
//            // add to lane group (as in lg.add_vehicle_packet)
//            wq.add_vehicle(vehicle);
//        }
//
//    }


//    /**
//     *  Clear all demands in the scenario.
//     */
//    public void clear_all_demands(){
//
//        // delete sources from links
//        for(Link link : network.links.values()) {
//            if(!link.has_demands())
//                continue;
//            link.get_demandGenerators().clear();
//        }
//
//        // delete all EventCreateVehicle and EventDemandChange from dispatcher
//        if(dispatcher!=null) {
//            dispatcher.remove_events_of_type(EventCreateVehicle.class);
//            dispatcher.remove_events_of_type(EventDemandChange.class);
//        }
//
//    }

}
