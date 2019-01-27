/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package common;

import error.OTMErrorLog;
import error.OTMException;
import geometry.AddLanes;
import geometry.FlowDirection;
import geometry.RoadGeometry;
import geometry.Side;
import jaxb.Roadparam;
import models.AbstractLaneGroup;
import models.AbstractModel;
import models.ctm.Model_CTM;
import models.micro.Model_Micro;
import models.pq.Model_PQ;
import runner.RunParameters;
import runner.Scenario;
import utils.OTMUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class Network {

    public static Long max_rcid;

    public Scenario scenario;
    public Map<Long,Node> nodes;
    public Map<Long,Link> links;
    public Map<Long, RoadGeometry> road_geoms;
    public Map<Long,jaxb.Roadparam> road_params;    // keep this for the sake of the scenario splitter
    public Map<Long,RoadConnection> road_connections = new HashMap<>();

    public Map<String,AbstractModel> models;

    ///////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public Network(Scenario scenario){
        this.scenario = scenario;
        nodes = new HashMap<>();
        links = new HashMap<>();
    }

    public Network(Scenario scenario,List<jaxb.Model> jaxb_models,List<jaxb.Node> jaxb_nodes, List<jaxb.Link> jaxb_links, jaxb.Roadgeoms jaxb_geoms, jaxb.Roadconnections jaxb_conns, jaxb.Roadparams jaxb_params) throws OTMException {

        this(scenario);

        nodes = read_nodes(jaxb_nodes,this);
        road_params = read_params(jaxb_params);
        road_geoms = read_geoms(jaxb_geoms);

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
        for(RoadConnection rc : road_connections.values())
            link2outrcs.get(rc.get_start_link_id()).add(rc);

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

        assign_road_params(jaxb_links,links,road_params);

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

    private static HashMap<Long,RoadGeometry> read_geoms(jaxb.Roadgeoms jaxb_geoms) {
        HashMap<Long,RoadGeometry> road_geoms = new HashMap<>();
        if(jaxb_geoms!=null) {
            for (jaxb.Roadgeom jaxb_geom : jaxb_geoms.getRoadgeom())
                road_geoms.put(jaxb_geom.getId(), new RoadGeometry(jaxb_geom));
        }
        return road_geoms;
    }

    private static Map<String,AbstractModel> generate_models(List<jaxb.Model> jaxb_models, Map<Long,Link> links) throws OTMException {

        if(jaxb_models==null)
            return new HashMap<>();

        Map<String,AbstractModel> models = new HashMap<>();
        Map<String,Set<Link>> model2links = new HashMap<>();
        Set<Link> all_links = new HashSet<>();

        boolean has_default_model = false;

        for(jaxb.Model jaxb_model : jaxb_models ){

            String name = jaxb_model.getName();

            if(model2links.containsKey(name))
                throw new OTMException("Duplicate model name.");

            AbstractModel model;
            switch(jaxb_model.getType()){
                case "ctm":
                    model = new Model_CTM( jaxb_model.getName(),
                                        jaxb_model.isIsDefault(),
                                        jaxb_model.getModelParams().getSimDt(),
                                        jaxb_model.getModelParams().getMaxCellLength());
                    break;

                case "point_queue":
                    model = new Model_PQ(jaxb_model.getName(),
                                        jaxb_model.isIsDefault());
                    break;

                case "micro":
                    model = new Model_Micro(jaxb_model.getName(),
                                        jaxb_model.isIsDefault(),
                                        jaxb_model.getModelParams().getSimDt());
                    break;

                default:
                    continue;
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
            all_links.addAll(my_links);

            model2links.put(model.name,my_links);

        }

        // set link models (links will choose new over default, so this determines the link list for each model)
        for( AbstractModel model : models.values())
            for (Link link : model2links.get(model.name))
                link.set_model(model);

        // Set links for each model
        for( AbstractModel model : models.values())
            model.set_links( all_links.stream().filter(link->link.model==model).collect(toSet()) );

        return models;
    }

    private static HashMap<Long,RoadConnection> read_road_connections(jaxb.Roadconnections jaxb_conns,Map<Long,Link> links) {

        HashMap<Long,RoadConnection> road_connections = new HashMap<>();
        Set<Long> no_road_connection = new HashSet<>();
        no_road_connection.addAll(links.values().stream().filter(x->!x.is_sink).map(y->y.getId()).collect(toSet()));
        if (jaxb_conns != null && jaxb_conns.getRoadconnection() != null) {
            for (jaxb.Roadconnection jaxb_rc : jaxb_conns.getRoadconnection()) {
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
        int start_lane = 1;
        int lanes;
        Long rc_id;
        RoadConnection newrc;
        Link end_link = link.end_node.out_links.values().iterator().next();
        int end_link_lanes = end_link.get_num_up_lanes();

        // dn in rc
        if(link.road_geom!=null && link.road_geom.dn_in!=null){
            rc_id = ++max_rcid;
            lanes = link.road_geom.dn_in.lanes;
            newrc = new RoadConnection(rc_id,link,start_lane,start_lane+lanes-1,end_link,1,end_link_lanes);
            new_rcs.put(rc_id, newrc);
            start_lane += lanes;
        }

        // stay rc
        rc_id = ++max_rcid;
        lanes = link.full_lanes;
        newrc = new RoadConnection(rc_id,link,start_lane,start_lane+lanes-1,end_link,1,end_link_lanes);
        new_rcs.put(rc_id, newrc);
        start_lane += lanes;

        // dn out rc
        if(link.road_geom!=null && link.road_geom.dn_out!=null){
            rc_id = ++max_rcid;
            lanes = link.road_geom.dn_out.lanes;
            newrc = new RoadConnection(rc_id,link,start_lane,start_lane+lanes-1,end_link,1,end_link_lanes);
            new_rcs.put(rc_id, newrc);
        }
        return new_rcs;
    }

    private static void create_lane_groups(Link link,final Set<RoadConnection> out_rcs) throws OTMException {

        // absent road connections: create them, if it is not a sink
        if(out_rcs.isEmpty() && !link.is_sink)
            throw new OTMException("THIS SHOULD NOT HAPPEN.");

        // create lanegroups
        link.set_long_lanegroups(create_dnflw_lanegroups(link,out_rcs));
        create_up_side_lanegroups(link);

        // set start_lane_up
        int up_in_lanes = link.road_geom!=null && link.road_geom.up_in!=null ? link.road_geom.up_in.lanes : 0;
        int dn_in_lanes = link.road_geom!=null && link.road_geom.dn_in!=null ? link.road_geom.dn_in.lanes : 0;
        int offset = dn_in_lanes-up_in_lanes;
        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values())
            if(lg.side==Side.stay)
                lg.start_lane_up = lg.start_lane_dn - offset;

        // set neighbors

        // .................. lat lanegroups = {up addlane}
        if(link.lanegroup_flwside_in !=null){
            AbstractLaneGroup inner_full = link.get_inner_full_lanegroup();
            link.lanegroup_flwside_in.neighbor_out = inner_full;
            inner_full.neighbor_up_in = link.lanegroup_flwside_in;
        }

        if (link.lanegroup_flwside_out != null) {
            AbstractLaneGroup outer_full = link.get_outer_full_lanegroup();
            link.lanegroup_flwside_out.neighbor_in = outer_full;
            outer_full.neighbor_up_out = link.lanegroup_flwside_out;
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

        // empty out_rc => sink
        if(out_rcs.isEmpty()){
            assert(link.is_sink);
            lanegroups.add(create_dnflw_lanegroup(link,1, link.full_lanes, null));
            return lanegroups;
        }

        // faster code for singleton
        if(out_rcs.size()==1) {
            lanegroups.add(create_dnflw_lanegroup(link, 1, link.full_lanes, out_rcs));
            return lanegroups;
        }

        // create map from lanes to road connection sets
        boolean lane_one_is_empty = false;
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
                else
                    lane_one_is_empty = true;
            }
            dnlane2rcs.put(lane,myrcs);
        }

        // case no lane groups for lane 1
        if(lane_one_is_empty){
            if(link.get_num_dn_lanes()<2)
                throw new OTMException(String.format("No outgoing road connection for link %d",link.getId()));
            dnlane2rcs.get(1).addAll(dnlane2rcs.get(2));
        }

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

        // Determine whether it is an addlane lanegroup or a stay lane group.
        Set<Side> sides = new HashSet<>();
        for(int lane=dn_start_lane;lane<dn_start_lane+num_lanes;lane++)
            sides.add(link.get_side_for_dn_lane(lane));

        if(sides.size()!=1)
            throw new OTMException(String.format("Rule broken: Lane groups must be contained in addlanes or stay lanes. Check link %d",link.getId()));

        float length = 0f;
        Side side = sides.iterator().next();
        switch(side){
            case in:
                length = link.road_geom.dn_in.length;
                break;
            case stay:
                length = link.length;
                break;
            case out:
                length = link.road_geom.dn_out.length;
                break;
        }

        return link.model.create_lane_group(link,side, FlowDirection.dn,length,num_lanes,dn_start_lane,out_rcs);
    }

    private static void create_up_side_lanegroups(Link link) throws OTMException {
        if(link.road_geom==null)
            return;
        if(link.road_geom.up_in!=null)
            link.lanegroup_flwside_in = create_up_side_lanegroup(link, link.road_geom.up_in);
        if(link.road_geom.up_out!=null)
            link.lanegroup_flwside_out = create_up_side_lanegroup(link,link.road_geom.up_out);
    }

    private static AbstractLaneGroup create_up_side_lanegroup(Link link, AddLanes addlanes) {
        float length = addlanes.length;
        int num_lanes = addlanes.lanes;
        Side side = addlanes.side;
        int start_lane_up = side==Side.in ? 1 : link.get_num_up_lanes() - addlanes.lanes + 1;

        return link.model.create_lane_group(link,side, FlowDirection.up,length,num_lanes,start_lane_up,null);
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
