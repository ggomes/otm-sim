package common;

import error.OTMErrorLog;
import error.OTMException;
import geometry.*;
import jaxb.Commodity;
import models.AbstractModel;
import models.fluid.ctm.ModelCTM;
import models.none.ModelNone;
import models.vehicle.newell.ModelNewell;
import models.vehicle.spatialq.ModelSpatialQ;
import plugin.PluginLoader;
import runner.RunParameters;
import utils.OTMUtils;
import utils.StochasticProcess;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class Network {

    protected static Long max_rcid;

    public Scenario scenario;
    public boolean node_positions_in_meters;    // true->meters, false->gps

    public Map<Long,jaxb.Roadparam> road_params;    // keep this for the sake of the scenario splitter
    public Map<String, AbstractModel> models; // TODO: MAKE THIS A SCENARIO ELEMENT

    // scenario elements
    public Map<Long,Node> nodes;
    public Map<Long,Link> links;
    public Map<Long, RoadGeometry> road_geoms;
    protected Map<Long,RoadConnection> road_connections;

    ///////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public Network(Scenario scenario){
        this.scenario = scenario;
        nodes = new HashMap<>();
        links = new HashMap<>();
    }

    public Network(Scenario scenario, List<jaxb.Commodity> jaxb_comms, List<jaxb.Model> jaxb_models, jaxb.Nodes jaxb_nodes, List<jaxb.Link> jaxb_links, jaxb.Roadgeoms jaxb_geoms, jaxb.Roadconnections jaxb_conns, jaxb.Roadparams jaxb_params,boolean jaxb_only) throws OTMException {

        this(scenario);

        node_positions_in_meters = jaxb_nodes.getGpsOrMeters().equalsIgnoreCase("meters");
        nodes = read_nodes(jaxb_nodes.getNode(),this);
        road_params = read_params(jaxb_params);
        road_geoms = read_geoms(jaxb_geoms,road_params);
        links = create_links(jaxb_links,this,nodes);

        if(!jaxb_only)
            nodes.values().stream().forEach(node -> node.is_many2one = node.out_links.size()==1);

        // is_source and is_sink
        if(!jaxb_only)
            for(Link link : links.values()){
                link.is_source = link.start_node.in_links.isEmpty();
                link.is_sink = link.end_node.out_links.isEmpty();
            }

        // allocate split matrix
        if(jaxb_comms!=null){
            Set<Long> pathless_comms = jaxb_comms.stream()
                    .filter(c->!c.isPathfull())
                    .map(c->c.getId())
                    .collect(toSet());

            links.values().stream()
                    .filter(link -> !link.is_sink && link.end_node.out_links.size()>1)
                    .forEach(link -> link.allocate_splits(pathless_comms));
        }

        // read road connections (requires links)
        road_connections = read_road_connections(jaxb_conns,links);

        // ignore the rest if we are not interested in lane groups
        if(jaxb_only)
            return;

        // store list of road connections in nodes
        for(RoadConnection rc : road_connections.values()) {
            if(rc.start_link!=null){
                rc.start_link.end_node.add_road_connection(rc);
            } else if(rc.end_link!=null){
                rc.end_link.start_node.add_road_connection(rc);
            } else {
                System.err.println("bad road connection: id=" + rc.getId());
            }
        }

        // assign models to links
        models = generate_models(jaxb_models,links);

        // create lane groups .......................................

        // call create_lane_groups
        for(Link link : links.values()) {
            Set<RoadConnection> out_rc = link.end_node.road_connections.stream()
                    .filter(rc->rc.start_link==link)
                    .collect(toSet());
            create_lane_groups(link, out_rc);
        }

        // set out lanegroups on road connections
        for(RoadConnection rc : road_connections.values()) {
            if (rc.end_link != null) {
                rc.out_lanegroups = new HashSet<>();
                // TODO THIS SEEMS SLOW
                for (int lane = rc.end_link_from_lane; lane <= rc.end_link_to_lane; lane++)
                    rc.out_lanegroups.add(rc.end_link.get_lanegroup_for_up_lane(lane));
            }
        }

        // populate link.outlink2lanegroups
        for(Link link : links.values()){

            if(link.is_sink)
                continue;

            link.outlink2lanegroups = new HashMap<>();
            for(Link outlink : link.end_node.out_links) {
                Set<AbstractLaneGroup> lgs = link.lanegroups_flwdn.stream()
                        .filter(lg -> lg.outlink2roadconnection.containsKey(outlink.getId()))
                        .collect(Collectors.toSet());
                if(!lgs.isEmpty())
                    link.outlink2lanegroups.put(outlink.getId(), lgs);
            }
        }

    }


    /////////////////////////////////////////////////
    // InterfaceScenarioElement-like
    /////////////////////////////////////////////////

    public void validate(OTMErrorLog errorLog){
        nodes.values().forEach(x->x.validate(errorLog));
        links.values().forEach(x->x.validate(errorLog));
        road_geoms.values().forEach(x->x.validate(errorLog));
        road_connections.values().forEach(x->x.validate(errorLog));
        models.values().forEach(x->x.validate(errorLog));
    }

    public void initialize(Scenario scenario,float start_time) throws OTMException {

        for(Link link : links.values())
            link.initialize(scenario,start_time);

        for(AbstractModel model : models.values())
            model.initialize(scenario);
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

    /////////////////////////////////////////////////
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
                    model = new ModelCTM(jaxb_model.getName(),
                                         jaxb_model.isIsDefault(),
                                         process,
                                         jaxb_model.getModelParams());
                    break;

                case "spaceq":
                    model = new ModelSpatialQ(jaxb_model.getName(),
                                        jaxb_model.isIsDefault(),
                                        process,
                                        null);
                    break;

                case "micro":
                    model = new ModelNewell(jaxb_model.getName(),
                            jaxb_model.isIsDefault(),
                            process,
                            jaxb_model.getModelParams() );

                    break;

                case "none":
                    model = new ModelNone(jaxb_model.getName(),
                                        jaxb_model.isIsDefault(),
                                        process,
                                        null );

                    nonemodel = (ModelNone) model;
                    break;

                default:

                    // it might be a plugin
                    model = PluginLoader.get_model_instance(jaxb_model,process);

                    if(model==null)
                        throw new OTMException("Bad model type: " + jaxb_model.getType());
                    break;

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
                nonemodel = new ModelNone("none", false, StochasticProcess.deterministic,null);
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

    private static HashMap<Long,RoadConnection> read_road_connections(jaxb.Roadconnections jrcs,Map<Long,Link> links) throws OTMException {

        // read jaxb road connections
        HashMap<Long,RoadConnection> rcs = new HashMap<>();
        Set<Long> no_rc = new HashSet<>();
        no_rc.addAll(links.values().stream().filter(x->!x.is_sink).map(y->y.getId()).collect(toSet()));
        if (jrcs != null && jrcs.getRoadconnection() != null) {
            for (jaxb.Roadconnection jrc : jrcs.getRoadconnection()) {
                if(rcs.containsKey(jrc.getId()))
                    throw new OTMException("Repeated road connection id");
                RoadConnection rc =  new RoadConnection(links, jrc);
                rcs.put(jrc.getId(),rc);
                no_rc.remove(rc.get_start_link_id());
            }
        }

        // create road connections for non-sink links with no road connections
        max_rcid = rcs.isEmpty() ? 0L : rcs.keySet().stream().max(Long::compareTo).get();
        for(Long link_id : no_rc){
            Link link = links.get(link_id);
            Map<Long,RoadConnection> new_rcs = new HashMap<>();
            for(Link end_link : link.end_node.out_links){
                RoadConnection rc_stay = new RoadConnection(
                        ++max_rcid,
                        link,
                        1,
                        link.get_num_dn_lanes(),
                        end_link,
                        1,
                        end_link.get_num_up_lanes() );
                new_rcs.put(rc_stay.id, rc_stay);
            }
            rcs.putAll(new_rcs);
        }

        return rcs;
    }

    private static void create_lane_groups(Link link,final Set<RoadConnection> out_rcs) throws OTMException {

        if (link.model == null)
            throw new OTMException("Not all links have a model.");

        if (out_rcs.isEmpty() && !link.is_sink)
            throw new OTMException("out_rcs.isEmpty() && !link.is_sink FOR LINK "+link.id);

        // create lanegroups
        link.set_flwdn_lanegroups(create_dnflw_lanegroups(link, out_rcs));

        // set start_lane_up ...................
        int offset = 0;
        if(link.road_geom!=null){
            if(link.road_geom.in_is_full_length())
                offset = 0;
            else {
                int dn_in_lanes = link.road_geom.dn_in != null ? link.road_geom.dn_in.lanes : 0;
                offset = dn_in_lanes;
            }
        }

        for (AbstractLaneGroup lg : link.lanegroups_flwdn) {
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

        // set neighbors ...................

        // ................... long lanegroups = {dn addlane, stay lgs}
        int num_dn_lanes = link.get_num_dn_lanes();
        if(num_dn_lanes>1) {
            List<AbstractLaneGroup> long_lgs = IntStream.rangeClosed(1, link.get_num_dn_lanes())
                    .mapToObj(lane -> link.dnlane2lanegroup.get(lane)).collect(toList());
            AbstractLaneGroup prev_lg = null;
            for (int lane = 1; lane <= num_dn_lanes; lane++) {

                AbstractLaneGroup lg = long_lgs.get(lane - 1);

                assert(lg!=null);

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

        // set barriers .................
        if(link.road_geom!=null){
            if(link.road_geom.dn_in!=null && !link.road_geom.dn_in.isopen)
                link.in_barriers = generate_barriers(link,link.road_geom.dn_in);
            if(link.road_geom.dn_out!=null && !link.road_geom.dn_out.isopen)
                link.out_barriers = generate_barriers(link,link.road_geom.dn_out);
        }

    }

    // called once only. Creates list of lanegroups ordered from inner to outer
    private static List<AbstractLaneGroup> create_dnflw_lanegroups(Link link, Set<RoadConnection> out_rcs) throws OTMException {
        // Find unique subsets of road connections, and create a lane group for each one.

        List<AbstractLaneGroup> lanegroups = new ArrayList<>();

        // empty out_rc <=> sink
        assert(out_rcs.isEmpty()==link.is_sink);

        int start_lane = 1;

        // inner addlane ..................................
        if(link.road_geom!=null && link.road_geom.dn_in!=null){

            // collect road connections for this addlane
            final int end_lane = link.road_geom.dn_in.lanes;
            Set<RoadConnection> myrcs = out_rcs.stream()
                    .filter(rc->rc.start_link_from_lane <= end_lane)
                    .collect(toSet());

            // add lanes have either no road connection or all
            // road connections span all lanes.
            if(myrcs!=null && !myrcs.isEmpty() && !myrcs.stream().allMatch(rc-> rc.start_link_from_lane==1 && rc.start_link_to_lane>=end_lane))
                throw new OTMException("Road connections do not conform to rules.");

            // create the lane group
            lanegroups.add(create_dnflw_lanegroup(link,
                    1,
                    link.road_geom.dn_in.lanes,
                    myrcs));

            start_lane = end_lane + 1;
        }

        // middle lanes .................................
        final int fstartlane = start_lane;
        Set<RoadConnection> prevrcs = out_rcs.stream()
                .filter(rc->rc.start_link_from_lane <= fstartlane &&
                        rc.start_link_to_lane >= fstartlane)
                .collect(toSet());

        int lg_start_lane = start_lane;
        int lane;
        for(lane=start_lane+1;lane<start_lane+link.full_lanes;lane++) {
            final int flane = lane;
            Set<RoadConnection> myrcs = out_rcs.stream()
                    .filter(rc->rc.start_link_from_lane <= flane &&
                                rc.start_link_to_lane >= flane)
                    .collect(toSet());
            if(!myrcs.equals(prevrcs)){
                lanegroups.add(create_dnflw_lanegroup(link,
                        lg_start_lane,
                        lane-lg_start_lane,
                        myrcs));
                prevrcs = myrcs;
                lg_start_lane = lane;
            }
        }

        lanegroups.add(create_dnflw_lanegroup(link,
                lg_start_lane,
                lane-lg_start_lane,
                prevrcs));

        // outer addlane ..................................
        if(link.road_geom!=null && link.road_geom.dn_out!=null){

            final int fstart_lane = start_lane + link.full_lanes;
            final int fend_lane = fstart_lane + link.road_geom.dn_out.lanes -1;

            // collect road connections for this addlane
            final int end_lane = start_lane+link.road_geom.dn_out.lanes-1;
            Set<RoadConnection> myrcs = out_rcs==null ? null : out_rcs.stream()
                    .filter(rc->rc.start_link_to_lane >= fstart_lane )
                    .collect(toSet());

            // add lanes have either no road connection or all
            // road connections span all lanes.
            if(myrcs!=null && !myrcs.isEmpty() && !myrcs.stream().allMatch(rc-> rc.start_link_from_lane<=fstart_lane && rc.start_link_to_lane==fend_lane))
                throw new OTMException("Road connections do not conform to rules.");

            // create the lane group
            lanegroups.add(create_dnflw_lanegroup(link,
                    fstart_lane,
                    link.road_geom.dn_out.lanes,
                    myrcs));

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
//        if(sides.size()!=1)
//            throw new OTMException(String.format("Rule broken: Lane groups must be contained in addlanes or stay lanes. Check link %d",link.getId()));

        jaxb.Roadparam rp = null;
        float length = 0f;
        Side side = sides.iterator().next();
        switch(side){
            case in:    // inner addlane lane group
                rp = link.road_geom.dn_in.roadparam;
                length = link.road_geom.dn_in.get_length(link.length);
                break;
            case middle:    // full lane lane group
                rp = link.road_param_full;
                length = link.length;
                break;
            case out:    // outer addlane lane group
                rp = link.road_geom.dn_out.roadparam;
                length = link.road_geom.dn_out.get_length(link.length);
                break;
        }

        // This precludes multiple lane groups of the same side: multiple 'stay' lane
        return link.model.create_lane_group(link,side,length,num_lanes,dn_start_lane,out_rcs,rp);
    }

    private static HashSet<Barrier> generate_barriers(Link link,AddLanes addlanes){
        HashSet<Barrier> X = new HashSet<>();
        List<Float> gate_points = new ArrayList<>();
        gate_points.add(0f);
        gate_points.add(link.length);
        for(Gate gate : addlanes.gates){
            gate_points.add(gate.start_pos);
            gate_points.add(gate.end_pos);
        }
        Collections.sort(gate_points);
        for(int i=0;i<gate_points.size();i+=2){
            float bstart = gate_points.get(i);
            float bend = gate_points.get(i+1);
            X.add(new Barrier(bstart,bend));
        }
        return X;
    }

    ////////////////////////////////////////////
    // get / set
    ///////////////////////////////////////////

//    public Set<AbstractLaneGroup> get_lanegroups(){
//        return links.values().stream().flatMap(link->link.lanegroups_flwdn.values().stream()).collect(toSet());
//    }

    public Collection<RoadConnection> get_road_connections(){
        return road_connections.values();
    }

    public RoadConnection get_road_connection(Long id){
        return road_connections.get(id);
    }

    ///////////////////////////////////////////
    // toString
    ///////////////////////////////////////////

    @Override
    public String toString() {
        return String.format("%d nodes, %d links",nodes.size(),links.size());
    }

}
