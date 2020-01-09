package common;

import error.OTMErrorLog;
import error.OTMException;
import geometry.AddLanes;
import geometry.FlowPosition;
import geometry.RoadGeometry;
import geometry.Side;
import jaxb.Roadparam;
import models.AbstractLaneGroup;
import models.AbstractModel;
import models.fluid.ctm.ModelCTM;
import models.none.ModelNone;
import models.vehicle.newell.ModelNewell;
import models.vehicle.spatialq.ModelSpatialQ;
import runner.RunParameters;
import runner.Scenario;
import utils.OTMUtils;
import utils.StochasticProcess;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class Network {

    public static Long max_rcid;

    public Scenario scenario;
    public Map<String, AbstractModel> models;
    public boolean node_positions_in_meters;    // true->meters, false->gps
    public Map<Long,Node> nodes;
    public Map<Long,Link> links;
    public Map<Long, RoadGeometry> road_geoms;
    public Map<Long,jaxb.Roadparam> road_params;    // keep this for the sake of the scenario splitter
    public Map<Long,RoadConnection> road_connections;

    ///////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public Network(Scenario scenario){
        this.scenario = scenario;
        nodes = new HashMap<>();
        links = new HashMap<>();
    }

    public Network(Scenario scenario,List<jaxb.Model> jaxb_models,jaxb.Nodes jaxb_nodes, List<jaxb.Link> jaxb_links, jaxb.Roadgeoms jaxb_geoms, jaxb.Roadconnections jaxb_conns, jaxb.Roadparams jaxb_params) throws OTMException {

        this(scenario);

        node_positions_in_meters = jaxb_nodes.getGpsOrMeters().equalsIgnoreCase("meters");
        nodes = read_nodes(jaxb_nodes.getNode(),this);
        road_params = read_params(jaxb_params);
        road_geoms = read_geoms(jaxb_geoms,road_params);

        links = create_links(jaxb_links,this,nodes);

        nodes.values().stream().forEach(node -> node.is_many2one = node.out_links.size()==1);

        // is_source and is_sink
        for(Link link : links.values()){
            link.is_source = link.start_node.in_links.isEmpty();
            link.is_sink = link.end_node.out_links.isEmpty();
        }

        // read road connections (requires links)
        road_connections = read_road_connections(jaxb_conns,links);

        // store list of road connections in nodes
        for(common.RoadConnection rc : road_connections.values()) {
            if(rc.has_start_link()){
                rc.get_start_link().end_node.add_road_connection(rc);
            } else if(rc.has_end_link()){
                rc.get_end_link().start_node.add_road_connection(rc);
            } else {
                System.err.println("bad road connection: id=" + rc.getId());
            }
        }

        // assign models to links
        models = generate_models(jaxb_models,links);

        // create lane groups .......................................

        // create link to road connections map
        Map<Long,Set<RoadConnection>> link2outrcs = new HashMap<>();
        links.keySet().forEach(link_id->link2outrcs.put(link_id,new HashSet<>()));
        for(RoadConnection rc : road_connections.values()){
            if(rc.start_link!=null && rc.end_link!=null) {
                link2outrcs.get(rc.start_link.id).add(rc);
                rc.start_link.outlink2roadconnection.put(rc.get_end_link_id(),rc);
            }
        }

        // call create_lane_groups
        for(Link link : links.values())
            create_lane_groups(link, link2outrcs.get(link.getId()));

        // Lanegroup connections .........................................................

        // set in/out lanegroups on road connections
        road_connections.values().forEach(rc->set_rc_in_out_lanegroups(rc));

        // populate link.outlink2lanegroups
        links.values().forEach(link->link.populate_outlink2lanegroups());

        // models .................................................
        models.values().forEach(x->x.build());

        // assign road params
        assign_road_params(jaxb_links,links,road_params);

    }

    // constructor for static scenario
    public Network(Scenario scenario,List<jaxb.Node> jaxb_nodes, List<jaxb.Link> jaxb_links, jaxb.Roadparams jaxb_params) throws OTMException {

        this(scenario);

        nodes = read_nodes(jaxb_nodes,this);
        road_params = read_params(jaxb_params);
        links = create_links(jaxb_links,this,nodes);

        nodes.values().stream().forEach(node -> node.is_many2one = node.out_links.size()==1);

        // is_source and is_sink
        for(Link link : links.values()){
            link.is_source = link.start_node.in_links.isEmpty();
            link.is_sink = link.end_node.out_links.isEmpty();
        }

//        assign_road_params(jaxb_links,links,road_params);

    }

    //////////////////////////////////////////////////
    // private static
    /////////////////////////////////////////////////

    private static HashMap<Long,Link> create_links(List<jaxb.Link> jaxb_links,Network network,Map<Long,Node> nodes) throws OTMException {
        HashMap<Long,Link> links = new HashMap<>();
        for( jaxb.Link jl : jaxb_links ) {
            long id = jl.getId();

            // check if we have the link id
            if( links.containsKey(id)  )
                throw new OTMException("Tried to add duplicate link id " + id );

            Link link = new Link(network,
                    network.road_params.get(jl.getRoadparam()),
                    id,
                    jl.getLength(),
                    jl.getFullLanes(),
                    nodes.get(jl.getStartNodeId()),
                    nodes.get(jl.getEndNodeId()) ,
                    jl.getRoadgeom()==null ? null : network.road_geoms.get(jl.getRoadgeom()),
                    jl.getRoadType()==null ? Link.RoadType.none : Link.RoadType.valueOf(jl.getRoadType()) ,
                    jl.getPoints()==null ? null : jl.getPoints() );

            links.put(id,link);
        }

        return links;
    }

    private static HashMap<Long,Node> read_nodes(List<jaxb.Node> jaxb_nodes,Network network) throws OTMException {
        HashMap<Long,Node> nodes = new HashMap<>();
        for( jaxb.Node jn : jaxb_nodes ) {
            long id = jn.getId();
            if( nodes.containsKey(id) )
                throw new OTMException("Tried to add duplicate node id " + id);
            nodes.put(id,new Node(network,jn));
        }
        return nodes;
    }

    private static HashMap<Long,jaxb.Roadparam> read_params(jaxb.Roadparams jaxb_params) {
        HashMap<Long,jaxb.Roadparam> road_params = new HashMap<>();
        if(jaxb_params!=null) {
            for (jaxb.Roadparam r : jaxb_params.getRoadparam())
                road_params.put(r.getId(), r);
        }
        return road_params;
    }

    private static HashMap<Long,RoadGeometry> read_geoms(jaxb.Roadgeoms jaxb_geoms,Map<Long,jaxb.Roadparam> road_params) throws OTMException {
        HashMap<Long,RoadGeometry> road_geoms = new HashMap<>();
        if(jaxb_geoms!=null) {
            for (jaxb.Roadgeom jaxb_geom : jaxb_geoms.getRoadgeom())
                road_geoms.put(jaxb_geom.getId(), new RoadGeometry(jaxb_geom,road_params));
        }
        return road_geoms;
    }

    private static Map<String, AbstractModel> generate_models(List<jaxb.Model> jaxb_models, Map<Long,Link> links) throws OTMException {

        if(jaxb_models==null) {
            jaxb_models = new ArrayList<>();
            jaxb.Model jaxb_model = new jaxb.Model();
            jaxb_models.add(jaxb_model);
            jaxb_model.setType("none");
            jaxb_model.setName("default none");
            jaxb_model.setIsDefault(true);
        }

        Map<String, AbstractModel> models = new HashMap<>();
        Map<String,Set<Link>> model2links = new HashMap<>();
        Set<Link> assigned_links = new HashSet<>();
        ModelNone nonemodel = null;

        boolean has_default_model = false;

        for(jaxb.Model jaxb_model : jaxb_models ){

            String name = jaxb_model.getName();

            if(model2links.containsKey(name))
                throw new OTMException("Duplicate model name.");

            StochasticProcess process;
            try {
                process = jaxb_model.getProcess()==null ? StochasticProcess.poisson : StochasticProcess.valueOf(jaxb_model.getProcess());
            } catch (IllegalArgumentException e) {
                process = StochasticProcess.poisson;
            }

            AbstractModel model;
            switch(jaxb_model.getType()){
                case "ctm":
                    model = new ModelCTM( jaxb_model.getName(),
                                        jaxb_model.isIsDefault(),
                                        jaxb_model.getModelParams().getSimDt(),
                                        process,
                                        jaxb_model.getModelParams().getMaxCellLength());
                    break;

                case "spaceq":
                    model = new ModelSpatialQ(jaxb_model.getName(),
                                        jaxb_model.isIsDefault(),
                                        process);
                    break;

                case "micro":
                    model = new ModelNewell(jaxb_model.getName(),
                                        jaxb_model.isIsDefault(),
                                        jaxb_model.getModelParams().getSimDt(),
                                        process);
                    break;

                case "none":
                    model = new ModelNone(jaxb_model.getName(),
                                        jaxb_model.isIsDefault(),
                                        process);
                    nonemodel = (ModelNone) model;
                    break;

                default:
                    throw new OTMException("Bad model type: " + jaxb_model.getType());

            }
            models.put(jaxb_model.getName(),model);

            // save the links for this model
            Set<Link> my_links = new HashSet<>();
            if(jaxb_model.isIsDefault()){
                if(has_default_model)
                    throw new OTMException("Multiple default models.");
                has_default_model = true;
                my_links.addAll(links.values());
            } else {
                List<Long> link_ids = OTMUtils.csv2longlist(jaxb_model.getLinks());
                for(Long link_id : link_ids){
                    if(!links.containsKey(link_id))
                        throw new OTMException("Unknown link id in model " + jaxb_model.getName());
                    my_links.add(links.get(link_id));
                }
            }
            assigned_links.addAll(my_links);

            model2links.put(model.name,my_links);

        }

        // assign 'none' model to remaining links
        if(assigned_links.size()<links.values().size()){
            Set<Link> my_links = new HashSet<>();
            my_links.addAll(links.values());
            my_links.removeAll(assigned_links);

            if(nonemodel==null) {
                if(models.containsKey("none"))
                    throw new OTMException("'none' is a prohibited name for a model.");
                nonemodel = new ModelNone("none", false, StochasticProcess.deterministic);
                models.put("none", nonemodel);
                model2links.put("none",my_links);
            } else {
                my_links.addAll(model2links.get(nonemodel.name));
                model2links.put(nonemodel.name,my_links);
            }
        }

        for( AbstractModel model : models.values())
            model.set_links(model2links.get(model.name));

        // set link models (links will choose new over default, so this determines the link list for each model)
        for( AbstractModel model : models.values()) {
            for (Link link : model2links.get(model.name)) {

                // determine whether link is a relative source link
                // (a relative source is one that has at least one incoming link that is not in the model)
                boolean incoming_are_all_in_model = model.links.containsAll(link.start_node.in_links.values());
                boolean is_model_source = !link.is_source && !incoming_are_all_in_model;
                link.set_model(model, is_model_source);
            }
        }

        return models;
    }

    private static HashMap<Long,RoadConnection> read_road_connections(jaxb.Roadconnections jaxb_conns,Map<Long,Link> links) throws OTMException {

        HashMap<Long,RoadConnection> road_connections = new HashMap<>();
        Set<Long> no_road_connection = new HashSet<>();
        no_road_connection.addAll(links.values().stream().filter(x->!x.is_sink).map(y->y.getId()).collect(toSet()));
        if (jaxb_conns != null && jaxb_conns.getRoadconnection() != null) {
            for (jaxb.Roadconnection jaxb_rc : jaxb_conns.getRoadconnection()) {
                if(road_connections.containsKey(jaxb_rc.getId()))
                    throw new OTMException("Repeated road connection id");
                RoadConnection rc =  new RoadConnection(links, jaxb_rc);
                road_connections.put(jaxb_rc.getId(),rc);
                no_road_connection.remove(rc.get_start_link_id());
            }
        }

        max_rcid = road_connections.isEmpty() ? 0L : road_connections.keySet().stream().max(Long::compareTo).get();

        // create absent road connections
        for(Long link_id : no_road_connection)
            road_connections.putAll(create_missing_road_connections(links.get(link_id)));

        return road_connections;
    }

    private static void assign_road_params(List<jaxb.Link> jaxb_links, Map<Long,Link> links, Map<Long, Roadparam> road_params) throws OTMException{
        for( jaxb.Link jl : jaxb_links ) {
            Link link = links.get(jl.getId());
            jaxb.Roadparam rp = road_params.get(jl.getRoadparam());
            if(rp==null)
                throw new OTMException("No road parameters for link id " + jl.getId()  );
            link.model.set_road_param(link,rp);
        }
    }

    private static Map<Long,RoadConnection> create_missing_road_connections(Link link){
        Map<Long,RoadConnection> new_rcs = new HashMap<>();
        int lanes;
        Long rc_id;

        for(Link end_link : link.end_node.out_links.values()){

            int start_lane = 1;
            int end_link_lanes = end_link.get_num_up_lanes();

            // dn in rc
            if(link.road_geom!=null && link.road_geom.dn_in!=null){
                rc_id = ++max_rcid;
                lanes = link.road_geom.dn_in.lanes;
                RoadConnection rc_dnin = new RoadConnection(rc_id,link,start_lane,start_lane+lanes-1,end_link,1,end_link_lanes);
                new_rcs.put(rc_id, rc_dnin);
                start_lane += lanes;
            }

            // stay rc
            rc_id = ++max_rcid;
            lanes = link.full_lanes;
            RoadConnection rc_stay = new RoadConnection(rc_id,link,start_lane,start_lane+lanes-1,end_link,1,end_link_lanes);
            new_rcs.put(rc_id, rc_stay);
            start_lane += lanes;

            // dn out rc
            if(link.road_geom!=null && link.road_geom.dn_out!=null){
                rc_id = ++max_rcid;
                lanes = link.road_geom.dn_out.lanes;
                RoadConnection rc_dnout = new RoadConnection(rc_id,link,start_lane,start_lane+lanes-1,end_link,1,end_link_lanes);
                new_rcs.put(rc_id, rc_dnout);
            }

        }


        return new_rcs;
    }

    private static void create_lane_groups(Link link,final Set<RoadConnection> out_rcs) throws OTMException {

        if (link.model == null)
            throw new OTMException("Not all links have a model.");

        // absent road connections: create them, if it is not a sink
        if (out_rcs.isEmpty() && !link.is_sink)
            throw new OTMException("out_rcs.isEmpty() && !link.is_sink FOR LINK "+link.id);

        // create lanegroups
        link.set_long_lanegroups(create_dnflw_lanegroups(link, out_rcs));
        create_up_side_lanegroups(link);

        int offset = 0;
        if(link.road_geom!=null){
            if(link.road_geom.in_is_full_length())
                offset = 0;
            else {
                int dn_in_lanes = link.road_geom.dn_in != null ? link.road_geom.dn_in.lanes : 0;
                int up_in_lanes = link.road_geom.up_in != null ? link.road_geom.up_in.lanes : 0;
                offset = dn_in_lanes-up_in_lanes;
            }
        }

        for (AbstractLaneGroup lg : link.lanegroups_flwdn.values()) {
            switch (lg.side) {
                case in:
                    if(link.road_geom.in_is_full_length())
                        lg.start_lane_up = lg.start_lane_dn;
                    break;
                case middle:
                    lg.start_lane_up = lg.start_lane_dn - offset;
                    break;
                case out:
                    if(link.road_geom.out_is_full_length())
                        lg.start_lane_up = lg.start_lane_dn - offset;
                    break;
            }
        }

        // set neighbors

        // .................. lat lanegroups = {up addlane}
        if(link.lanegroup_up_in !=null){
            AbstractLaneGroup inner_full = link.get_inner_full_lanegroup();
            link.lanegroup_up_in.neighbor_out = inner_full;
            inner_full.neighbor_up_in = link.lanegroup_up_in;
        }

        if (link.lanegroup_up_out != null) {
            AbstractLaneGroup outer_full = link.get_outer_full_lanegroup();
            link.lanegroup_up_out.neighbor_in = outer_full;
            outer_full.neighbor_up_out = link.lanegroup_up_out;
        }

        // ................... long lanegroups = {dn addlane, stay lgs}
        int num_dn_lanes = link.get_num_dn_lanes();
        if(num_dn_lanes>1) {
            List<AbstractLaneGroup> long_lgs = IntStream.rangeClosed(1, link.get_num_dn_lanes())
                    .mapToObj(lane -> link.dnlane2lanegroup.get(lane)).collect(toList());
            AbstractLaneGroup prev_lg = null;
            for (int lane = 1; lane <= num_dn_lanes; lane++) {

                AbstractLaneGroup lg = long_lgs.get(lane - 1);
                if (prev_lg == null)
                    prev_lg = lg;
                if (lg != prev_lg) {
                    lg.neighbor_in = prev_lg;
                    prev_lg.neighbor_out = lg;
                    prev_lg = lg;
                }
            }

            prev_lg = null;
            for(int lane=num_dn_lanes;lane>=1;lane--){
                AbstractLaneGroup lg = long_lgs.get(lane-1);
                if(prev_lg==null)
                    prev_lg = lg;
                if(lg!=prev_lg) {
                    lg.neighbor_out = prev_lg;
                    prev_lg.neighbor_in = lg;
                    prev_lg = lg;
                }
            }
        }
    }

    private static Set<AbstractLaneGroup> create_dnflw_lanegroups(Link link, Set<RoadConnection> out_rcs) throws OTMException {
        // Find unique subsets of road connections, and create a lane group for each one.

        Set<AbstractLaneGroup> lanegroups = new HashSet<>();

        // empty out_rc <=> sink
        assert(out_rcs.isEmpty()==link.is_sink);
//        if(out_rcs.isEmpty()){
//            assert(link.is_sink);
//            lanegroups.add(create_dnflw_lanegroup(link,1, link.full_lanes, null));
//            return lanegroups;
//        }

        // for sinks (or out_rcs is empty), create lane groups according to addlanes.
        if(link.is_sink){

            // trivial case
            if(link.road_geom==null) {
                lanegroups.add(create_dnflw_lanegroup(link, 1, link.full_lanes, null));
                return lanegroups;
            }

            int lane = 0;

            // create inner addlane
            if(link.road_geom.dn_in!=null) {
                lanegroups.add(create_dnflw_lanegroup(link, 1, link.road_geom.dn_in.lanes, null));
                lane += link.road_geom.dn_in.lanes;
            }

            // create full lanes
            if(link.full_lanes>0){
                lanegroups.add(create_dnflw_lanegroup(link, lane+1, link.full_lanes, null));
                lane += link.full_lanes;
            }

            // create outer addlane
            if(link.road_geom.dn_out!=null)
                lanegroups.add(create_dnflw_lanegroup(link, lane+1, link.road_geom.dn_out.lanes, null));

            return lanegroups;
        }

        // special code for singleton
        if(out_rcs.size()==1) {
            lanegroups.add(create_dnflw_lanegroup(link, 1, link.full_lanes, out_rcs));
            return lanegroups;
        }


        // create map from lanes to road connection sets
//        boolean lane_one_is_empty = false;
        Map<Integer,Set<RoadConnection>> dnlane2rcs = new HashMap<>();
        for(int lane=1;lane<=link.get_num_dn_lanes();lane++) {
            Set<RoadConnection> myrcs = new HashSet<>();
            for (RoadConnection rc : out_rcs){
                if (rc.start_link_from_lane <= lane && rc.start_link_to_lane >= lane)
                    myrcs.add(rc);
            }
            if(myrcs.isEmpty()) {
                if(lane>1)
                    myrcs.addAll(dnlane2rcs.get(lane-1));
//                else
//                    lane_one_is_empty = true;
            }
            dnlane2rcs.put(lane,myrcs);
        }

//        // case no lane groups for lane 1
//        if(lane_one_is_empty){
//            if(link.get_num_dn_lanes()<2)
//                throw new OTMException(String.format("No outgoing road connection for link %d",link.getId()));
//            dnlane2rcs.get(1).addAll(dnlane2rcs.get(2));
//        }

        // set of unique road connection sets
        Set<Set<RoadConnection>> unique_rc_sets = new HashSet<>();
        unique_rc_sets.addAll(dnlane2rcs.values());

        // create a lane group for each unique_rc_sets
        for(Set<RoadConnection> my_rcs : unique_rc_sets) {
            Set<Integer> lg_lanes = dnlane2rcs.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(my_rcs))
                    .map(entry->entry.getKey())
                    .collect(Collectors.toSet());
            int dn_start_lane = lg_lanes.stream().mapToInt(x->x).min().getAsInt();
            int num_lanes = lg_lanes.size();
            lanegroups.add(create_dnflw_lanegroup(link, dn_start_lane, num_lanes, my_rcs));
        }

        return lanegroups;
    }

    private static AbstractLaneGroup create_dnflw_lanegroup(Link link, int dn_start_lane, int num_lanes, Set<RoadConnection> out_rcs) throws OTMException {

        // Determine whether it is an addlane lanegroup or a full lane lane group.
        Set<Side> sides = new HashSet<>();
        for(int lane=dn_start_lane;lane<dn_start_lane+num_lanes;lane++)
            sides.add(link.get_side_for_dn_lane(lane));

        // all lanes must belong to one of the 3
        // That is, there are no lane groups that is both inner and full length, or outer and full length.
        if(sides.size()!=1)
            throw new OTMException(String.format("Rule broken: Lane groups must be contained in addlanes or stay lanes. Check link %d",link.getId()));

        float length = 0f;
        Side side = sides.iterator().next();
        switch(side){
            case in:    // inner addlane lane group
                length = Float.isNaN(link.road_geom.dn_in.length) ? link.length : link.road_geom.dn_in.length;
                break;
            case middle:    // full lane lane group
                length = link.length;
                break;
            case out:    // outer addlane lane group
                length = Float.isNaN(link.road_geom.dn_out.length) ? link.length : link.road_geom.dn_out.length;
                break;
        }

        // This precludes multiple lane groups of the same side: multiple 'stay' lane
        return link.model.create_lane_group(link,side, FlowPosition.dn,length,num_lanes,dn_start_lane,out_rcs);
    }

    private static void create_up_side_lanegroups(Link link) throws OTMException {
        if(link.road_geom==null)
            return;
        if(link.road_geom.up_in!=null)
            link.lanegroup_up_in = create_up_side_lanegroup(link, link.road_geom.up_in);
        if(link.road_geom.up_out!=null)
            link.lanegroup_up_out = create_up_side_lanegroup(link,link.road_geom.up_out);
    }

    private static AbstractLaneGroup create_up_side_lanegroup(Link link, AddLanes addlanes) {
        float length = addlanes.length;
        int num_lanes = addlanes.lanes;
        Side side = addlanes.side;
        int start_lane_up = side==Side.in ? 1 : link.get_num_up_lanes() - addlanes.lanes + 1;

        return link.model.create_lane_group(link,side, FlowPosition.up,length,num_lanes,start_lane_up,null);
    }

    private static void set_rc_in_out_lanegroups(RoadConnection rc){
        rc.in_lanegroups = rc.start_link !=null ?
                rc.start_link.get_unique_lanegroups_for_dn_lanes(rc.start_link_from_lane,rc.start_link_to_lane) :
                new HashSet<>();

        rc.out_lanegroups = rc.end_link!=null ?
                rc.end_link.get_unique_lanegroups_for_up_lanes(rc.end_link_from_lane,rc.end_link_to_lane) :
                new HashSet<>();
    }

    public void validate(Scenario scenario,OTMErrorLog errorLog){
        nodes.values().forEach(x->x.validate(scenario,errorLog));
        links.values().forEach(x->x.validate(errorLog));
        road_geoms.values().forEach(x->x.validate(errorLog));
        road_connections.values().forEach(x->x.validate(errorLog));
        models.values().forEach(x->x.validate(errorLog));
    }

    public void initialize(Scenario scenario,RunParameters runParams) throws OTMException {

        for(Link link : links.values())
            link.initialize(scenario,runParams);

        for(Node node: nodes.values())
            node.initialize(scenario,runParams);

        for(AbstractModel model : models.values())
            model.initialize(scenario);

    }

    ////////////////////////////////////////////
    // get / set
    ///////////////////////////////////////////

    public Set<AbstractLaneGroup> get_lanegroups(){
        return links.values().stream().flatMap(link->link.lanegroups_flwdn.values().stream()).collect(toSet());
    }

    public Collection<RoadConnection> get_road_connections(){
        return road_connections.values();
    }

    public RoadConnection get_road_connection(Long id){
        return road_connections.get(id);
    }

    @Override
    public String toString() {
        return String.format("%d nodes, %d links",nodes.size(),links.size());
    }

    public jaxb.Network to_jaxb(){
        jaxb.Network jnet = new jaxb.Network();

        // network: nodes
        jaxb.Nodes jnodes = new jaxb.Nodes();
        jnodes.setGpsOrMeters(node_positions_in_meters ? "meters" : "gps");
        jnet.setNodes(jnodes);
        for(Node node : nodes.values())
            jnodes.getNode().add(node.to_jaxb());

        // network: links
        jaxb.Links jlinks = new jaxb.Links();
        jnet.setLinks(jlinks);
        for(Link link : links.values())
            jlinks.getLink().add(link.to_jaxb());

        // network: roadgeoms
        jaxb.Roadgeoms jgeoms = new jaxb.Roadgeoms();
        jnet.setRoadgeoms(jgeoms);
        for(geometry.RoadGeometry geom : road_geoms.values())
            jgeoms.getRoadgeom().add(geom.to_jaxb());

        // network: roadconnections
        jaxb.Roadconnections jconns = new jaxb.Roadconnections();
        jnet.setRoadconnections(jconns);
        for(common.RoadConnection rcn : road_connections.values())
            jconns.getRoadconnection().add(rcn.to_jaxb());

        // network: roadparams
        jaxb.Roadparams jrpms = new jaxb.Roadparams();
        jnet.setRoadparams(jrpms);
        jrpms.getRoadparam().addAll(road_params.values());

        return jnet;
    }

}
