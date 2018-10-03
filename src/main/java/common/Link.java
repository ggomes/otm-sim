/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package common;

import commodity.Commodity;
import error.OTMErrorLog;
import error.OTMException;
import geometry.RoadGeometry;
import jaxb.Points;
import output.PathTravelTime;
import packet.PacketSplitter;
import runner.InterfaceScenarioElement;
import runner.RunParameters;
import runner.Scenario;
import runner.ScenarioElementType;

import java.util.*;

import static java.util.stream.Collectors.toSet;

public class Link implements InterfaceScenarioElement {

    public enum RoadType {none,onramp,offramp,freeway,arterial,hov,interconnect,source,sink,lightrail}
    public enum ModelType {pq,ctm,mn,micro,none}

    protected final long id;
    public Network network;

    // common parameters
    public final float length;          // meters
    public final int full_lanes;
    public common.Node start_node;
    public common.Node end_node;

    public boolean is_source;
    public boolean is_sink;

//    public int total_lanes;

    // lanegroups
    public Map<Long,AbstractLaneGroup> lanegroups;

    // downstream lane count -> lane group
    private Map<Integer, AbstractLaneGroup> dnlane2lanegroup;

    // map from path id (uses this link) to next link id (exits this link)
    public Map<Long,Long> path2outlink;

    // map from downstream link to candidate lanegroups
    public Map<Long, Set<AbstractLaneGroup>> outlink2lanegroups;

    public PacketSplitter packet_splitter;

    public Set<AbstractSource> sources;

    // commodities
    public Set<Commodity> commodities;

    // road type
    public RoadType road_type;

    // geometry
    public RoadGeometry road_geom;

    // road parameters (for the sake of writing again to jaxb)
    public Long road_param_id;

    // model
    public ModelType model_type;
    public AbstractLinkModel model;

    // flow accumulation
//    public Map<Long, FlowAccumulatorSet> commodity_flowaccumulators;

    // for path travel time output
    public Set<PathTravelTime> travel_timers;

    // shape (not used by otm)
    public List<Point> shape;

    ///////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public Link(Network network, Link.ModelType model_type, Long road_param_id, RoadGeometry rg, Link.RoadType road_type,long id, float length, int full_lanes, Points jpoints, Node start_node, Node end_node) throws OTMException {

        if (start_node == null)
            throw new OTMException("Unknown start node id in link " + id);

        if (end_node == null)
            throw new OTMException("Unknown end node id in link " + id);

        this.id = id;
        this.road_type = road_type==null ? RoadType.none : road_type;
        this.model_type = model_type == null ? ModelType.none : model_type;
        this.road_param_id = road_param_id;
        this.network = network;
        this.length = length;
        this.full_lanes = full_lanes;
        this.start_node = start_node;
        this.end_node = end_node;
        this.road_geom = rg;

        // source and sink. this is set later by the network
        this.is_source = true;
        this.is_sink = true;

        // node io
        this.start_node.add_output_link(this);
        this.end_node.add_input_link(this);

        this.path2outlink = new HashMap<>();

        // shape
        this.shape = new ArrayList<>();
        if (jpoints != null && jpoints.getPoint() != null)
            for (jaxb.Point jpoint : jpoints.getPoint())
                shape.add(new Point(jpoint.getX(), jpoint.getY()));
        else if (start_node.xcoord != null && start_node.ycoord != null && end_node.xcoord != null && end_node.ycoord != null) {
            shape.add(new Point(start_node.xcoord, start_node.ycoord));
            shape.add(new Point(end_node.xcoord, end_node.ycoord));
        }

        this.commodities = new HashSet<>();

//        this.total_lanes = road_geom==null ?
//                this.full_lanes :
//                Math.max(road_geom.dn_in.lanes, road_geom.up_in.lanes) + this.full_lanes + Math.max(road_geom.dn_out.lanes, road_geom.up_out.lanes);

        travel_timers = new HashSet<>();
    }

    public Link(Network network, Long road_param_id, long id, float length, int full_lanes, Node start_node, Node end_node) throws OTMException {

        if (start_node == null)
            throw new OTMException("Unknown start node id in link " + id);

        if (end_node == null)
            throw new OTMException("Unknown end node id in link " + id);

        this.id = id;
        this.road_type = road_type==null ? RoadType.none : road_type;
        this.model_type = model_type == null ? ModelType.none : model_type;
        this.road_param_id = road_param_id;
        this.network = network;
        this.length = length;
        this.full_lanes = full_lanes;
        this.start_node = start_node;
        this.end_node = end_node;
//        this.commodity_flowaccumulators = new HashMap<>();

        // node io
        this.start_node.add_output_link(this);
        this.end_node.add_input_link(this);

        this.path2outlink = new HashMap<>();

//        this.total_lanes = road_geom==null ?
//                this.full_lanes :
//                Math.max(road_geom.dn_in.lanes, road_geom.up_in.lanes) + this.full_lanes + Math.max(road_geom.dn_out.lanes, road_geom.up_out.lanes);
    }

    public void delete(){
        network = null;
        start_node = null;
        end_node = null;
        if(lanegroups!=null)
            lanegroups.values().forEach(lg->lg.delete());
        lanegroups = null;
        dnlane2lanegroup = null;
        path2outlink = null;
        outlink2lanegroups = null;
        packet_splitter = null;
        if(sources!=null)
            sources.forEach(s->s.delete());
        sources = null;
        commodities = null;
        road_type = null;
        road_geom = null;
        model_type = null;
        model = null;
//        commodity_flowaccumulators = null;
        travel_timers = null;
        shape = null;
    }

    public void add_commodity(Commodity commodity) {
        this.commodities.add(commodity);
        this.lanegroups.values().forEach(x -> x.add_commodity(commodity));
    }

    public void add_travel_timer(PathTravelTime x){
        travel_timers.add(x);
    }

    public void set_lanegroups(Set<AbstractLaneGroup> lgs) {

        // lanegroups
        lanegroups = new HashMap<>();
        for(AbstractLaneGroup lg : lgs)
            lanegroups.put(lg.id,lg);

        // dnlane2lanegroup
        dnlane2lanegroup = new HashMap<>();
        for (AbstractLaneGroup lg : lgs)
            for (int lane : lg.lanes)
                dnlane2lanegroup.put(lane, lg);
    }

    public void set_model(AbstractLinkModel model) throws OTMException {
        if (this.model != null)
            throw new OTMException("ModelType multiply assigned for link " + this.id);
        this.model = model;
    }

    public void add_source(AbstractSource source) {
        if(sources==null)
            sources =new HashSet<>();
        sources.add(source);
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
        if( lanegroups ==null )
            errorLog.addError("link " + id + ": lanegroups==null");

        // check that the road geometry fits the link
        if(this.road_geom!=null){

            // each addlane has length less than the link
            if(road_geom.up_in.length>this.length )
                errorLog.addError("link " + id + ", road_geom.up_in.length > this.length");
            if( road_geom.up_out.length>this.length )
                errorLog.addError("link " + id + ", road_geom.up_out.length > this.length");
            if( road_geom.dn_in.length>this.length )
                errorLog.addError("link " + id + ", road_geom.dn_in.length > this.length");
            if( road_geom.dn_out.length>this.length )
                errorLog.addError("link " + id + ", road_geom.dn_out.length > this.length");

            // sum of inside (outside) addlane lengths is less than link length
            if( road_geom.up_in.length+road_geom.dn_in.length>this.length)
                errorLog.addError("link " + id + ", road_geom.up_in.length+road_geom.dn_in.length>this.length");
            if( road_geom.up_out.length+road_geom.dn_out.length>this.length)
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
        if(lanegroups!=null && dnlane2lanegroup !=null) {
            Set<Long> A = dnlane2lanegroup.values().stream().map(x->x.id).collect(toSet());
            Set<Long> B = lanegroups.values().stream().map(x->x.id).collect(toSet());
            if (!A.equals(B))
                errorLog.addError("link " + id + ": not all lanegroups are represented in dnlane2lanegroup");
        }

        // lanegroups
        if(lanegroups!=null)
            lanegroups.values().forEach(x->x.validate(errorLog));

        // check that all lanes have a lanegroup
        // WARNING: Assumes no upstream addlanes
//        if(road_geom!=null && dnlane2lanegroup !=null)
//            for(int lane : this.get_exit_lanes() )
//                if(!dnlane2lanegroup.containsKey(lane))
//                    errorLog.addError("link " + id + ": !dnlane2lanegroup.containsKey(lane)");

        // model
        if(model!=null)
            model.validate(errorLog);

        // packet_splitter
        if(packet_splitter !=null)
            packet_splitter.validate(errorLog);

    }

    public void initialize(Scenario scenario, RunParameters runParams) throws OTMException {
        for(AbstractLaneGroup lg : lanegroups.values())
            lg.initialize(scenario,runParams);
        model.initialize(scenario);
//        if(is_source && sources!=null)
//            sources.forEach(x->x.initialize(scenario));
    }

    ////////////////////////////////////////////
    // get
    ///////////////////////////////////////////

    // WARNING: possible inefficiency if this is called a lot
    public List<Integer> get_entry_lanes(){
        // find first lane
        int first_lane = road_geom==null ? 1 : 1 + Math.max(0,road_geom.dn_in.lanes-road_geom.up_in.lanes);
        int last_lane = road_geom==null ? full_lanes : first_lane + road_geom.up_in.lanes + full_lanes + road_geom.up_out.lanes -1;
        List<Integer> lanes = new ArrayList<>();
        for(int lane=first_lane;lane<=last_lane;lane++)
            lanes.add(lane);
        return lanes;
    }

    public int get_num_dn_lanes(){
        return full_lanes +
                (road_geom.dn_in==null ? 0 : road_geom.dn_in.lanes) +
                (road_geom.dn_out==null ? 0 : road_geom.dn_out.lanes);
    }

    public int get_num_up_lanes(){
        return full_lanes +
                (road_geom.up_in==null ? 0 : road_geom.up_in.lanes) +
                (road_geom.up_out==null ? 0 : road_geom.up_out.lanes);
    }

//    // WARNING: possible inefficiency if this is called a lot
//    public List<Integer> get_exit_lanes(){
//        // find first lane
//        int first_lane = road_geom==null? 1 : 1 + Math.max(0,road_geom.up_in.lanes-road_geom.dn_in.lanes);
//        int last_lane = road_geom==null? full_lanes : first_lane + road_geom.dn_in.lanes + full_lanes + road_geom.dn_out.lanes -1;
//        List<Integer> lanes = new ArrayList<>();
//        for(int lane=first_lane;lane<=last_lane;lane++)
//            lanes.add(lane);
//        return lanes;
//    }

    // WARNING: Assumes no upstream add_lanes
    // returns length in meters
    public float get_length_for_lane(int lane){
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




//    public Set<AbstractLaneGroup> get_lanegroups_for_lanes(Set<Integer> lanes){
//        if(lanes==null)
//            return new HashSet(lanegroups.values());
//        Set<AbstractLaneGroup> r = new HashSet<>();
//        for(int lane : lanes)
//            r.add(dnlane2lanegroup.get(lane));
//        return r;
//    }

    public Set<AbstractLaneGroup> get_lanegroups_for_dn_lanes(int from_lane,int to_lane) {
        Set<AbstractLaneGroup> x = new HashSet<>();
        for (int lane = from_lane; lane <= to_lane; lane++)
            x.add(get_lanegroup_for_dn_lane(lane));
        return x;
    }

    public Set<AbstractLaneGroup> get_lanegroups_for_up_lanes(int from_lane,int to_lane) {
        Set<AbstractLaneGroup> x = new HashSet<>();
        for (int lane = from_lane; lane <= to_lane; lane++)
            x.add(get_lanegroup_for_up_lane(lane));
        return x;
    }

    public AbstractLaneGroup get_lanegroup_for_dn_lane(int lane){
        return dnlane2lanegroup.get(lane);
    }

    public AbstractLaneGroup get_lanegroup_for_up_lane(int lane){
        // TODO IMPLEMENT THIS
        return null;
    }

    public float get_max_vehicles(){
        return lanegroups.values().stream().
                map(x->x.max_vehicles).
                reduce(0f,(i,j)->i+j);
    }

    public double get_veh() {
        return lanegroups.values().stream()
                .mapToDouble(x->x.get_total_vehicles())
                .sum();
    }

    public double get_veh_for_commodity(Long commodity_id) {
        return lanegroups.values().stream()
                .mapToDouble(x->x.vehicles_for_commodity(commodity_id))
                .sum();
    }

    // links reached downstream by road connection
    public Set<Link> get_next_links(){
        if(outlink2lanegroups==null)
            return new HashSet<>();
        return outlink2lanegroups.keySet().stream().map(x->network.links.get(x)).collect(toSet());
    }

    public Collection<Link> get_previous_links(){
        return start_node.in_links.values();
    }

    public Set<RoadConnection> get_roadconnections_leaving(){
        Set<RoadConnection> rcs = new HashSet<>();
        for(AbstractLaneGroup lg : lanegroups.values())
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

    // return the instantaneous travel time averaged over the lane groups
    // This is used by the path travel timer.
    // It should be improved to measure travel times on the lanegroups used by the path.
    public double get_current_average_travel_time(){
        return lanegroups.values().stream().mapToDouble(lg->lg.get_current_travel_time()).average().getAsDouble();
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
        jlink.setRoadparam(this.road_param_id);
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

}
