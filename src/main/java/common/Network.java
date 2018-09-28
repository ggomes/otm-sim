/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package common;

import error.OTMErrorLog;
import error.OTMException;
import geometry.RoadGeometry;
import models.ctm.NodeModel;
import models.ctm.UpLaneGroup;
import packet.PacketLink;
import runner.RunParameters;
import runner.Scenario;
import utils.OTMUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

public class Network {

    private Long max_rcid;

    public Scenario scenario;
    public Map<Long,Node> nodes = new HashMap<>();
    public Map<Long,Link> links = new HashMap<>();
    public Map<Long, RoadGeometry> road_geoms = new HashMap<>();
    public Map<Long,jaxb.Roadparam> road_params = new HashMap<>();    // keep this for the sake of the scenario splitter

    private Map<Long,RoadConnection> road_connections = new HashMap<>();

    public Set<models.ctm.LinkModel> macro_link_models = new HashSet<>();
    public Set<Node> macro_internal_nodes = new HashSet<>();
    public Set<models.ctm.Source> macro_sources = new HashSet<>();

    ///////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public Network(Scenario scneario){
        this.scenario = scneario;
        nodes = new HashMap<>();
        links = new HashMap<>();
    }

    public Network(Scenario scenario,List<jaxb.Node> jaxb_nodes, List<jaxb.Link> jaxb_links, jaxb.Model model, jaxb.Roadgeoms jaxb_geoms, jaxb.Roadconnections jaxb_conns, jaxb.Roadparams jaxb_params) throws OTMException {

        this(scenario);

        // read nodes
        for( jaxb.Node jn : jaxb_nodes ) {
            long id = jn.getId();
            if( nodes.containsKey(id) )
                throw new OTMException("Tried to add duplicate node id " + id);
            nodes.put(id,new Node(this,jn));
        }

        // read road geoms
        road_geoms = new HashMap<>();
        if(jaxb_geoms!=null)
            for(jaxb.Roadgeom jaxb_geom : jaxb_geoms.getRoadgeom())
                road_geoms.put(jaxb_geom.getId(),new RoadGeometry(jaxb_geom));

        // specified models
        Map<Long,Link.ModelType> specified_models = new HashMap<>();
        if(model!=null){

            // point-queue
            if (model.getPointQueue()!=null) {
                List<Long> ids = OTMUtils.csv2longlist(model.getPointQueue().getContent());
                ids.forEach(x -> specified_models.put(x, Link.ModelType.pq));
            }

            // ctm
            if (model.getCtm()!=null) {
                List<Long> ids = OTMUtils.csv2longlist(model.getCtm().getContent());
                ids.forEach(x -> specified_models.put(x, Link.ModelType.ctm));
            }

            // mn
            if (model.getMn()!=null) {
                List<Long> ids = OTMUtils.csv2longlist(model.getMn().getContent());
                ids.forEach(x -> specified_models.put(x, Link.ModelType.mn));
            }

        }

        // create links
        macro_link_models = new HashSet<>();
        for( jaxb.Link jl : jaxb_links ) {
            long id = jl.getId();

            // check if we have the link id
            if( links.containsKey(id)  )
                throw new OTMException("Tried to add duplicate link id " + id );

            // get its type. if not specified, it is models.ctm.pq
            Link.ModelType my_model_type = specified_models.containsKey(id) ? specified_models.get(id) : Link.ModelType.none;

            Link link = new Link(this,
                    my_model_type,
                    jl.getRoadparam() ,
                    jl.getRoadgeom()==null ? null : road_geoms.get(jl.getRoadgeom()),
                    jl.getRoadType()==null ? Link.RoadType.none : Link.RoadType.valueOf(jl.getRoadType()) ,
                    id,
                    jl.getLength(),
                    jl.getFullLanes(),
                    jl.getPoints(),
                    nodes.get(jl.getStartNodeId()),
                    nodes.get(jl.getEndNodeId()) );

            // set model
            switch(my_model_type) {

                case pq:
                    models.pq.LinkModel pq_model = new models.pq.LinkModel(link);
                    link.set_model(pq_model);
                    break;

                case ctm:
                    models.ctm.LinkModel ctm_model = new models.ctm.LinkModel(link);
                    link.set_model(ctm_model);
                    macro_link_models.add(ctm_model);
                    break;

                case mn:
                    models.ctm.LinkModel mn_model = new models.ctm.LinkModel(link);
                    link.set_model(mn_model);
                    macro_link_models.add(mn_model);
                    break;

                case none:
                    models.none.LinkModel none_model = new models.none.LinkModel(link);
                    link.set_model(none_model);
                    break;

            }
            links.put(id,link);

        }

        // nodes is_many2one
//        nodes.values().stream().forEach(node -> node.is_many2one = node.in_links.size()==1 && node.out_links.size()==1);
        nodes.values().stream().forEach(node -> node.is_many2one = node.out_links.size()==1);

        // abort if we have macro links but were not given a sim_dt
        if( !macro_link_models.isEmpty() && Float.isNaN(scenario.sim_dt) )
            throw new OTMException("Attempted to load a scenario with macroscopic links, but did not provide a simulation time step.");

        // read road connections
        road_connections = new HashMap<>();
        if(jaxb_conns!=null && jaxb_conns.getRoadconnection()!=null)
            for(jaxb.Roadconnection jaxb_rc : jaxb_conns.getRoadconnection() )
                road_connections.put(jaxb_rc.getId(),new RoadConnection(this.links,jaxb_rc));

        max_rcid = road_connections.isEmpty() ? 0L : road_connections.keySet().stream().max(Long::compareTo).get();

        // create lane groups .......................................
        for(Link link : links.values()){   // not parallelizable

            // set sources and sinks according to incoming and outgoing links
            link.is_source = link.start_node.in_links.isEmpty();
            link.is_sink = link.end_node.out_links.isEmpty();

            Set<AbstractLaneGroup> lgs = create_lanegroups_from_roadconnections(link);

            // send them to the link
            link.set_lanegroups(lgs);

        }

        // set in/out lanegroups on road connections
        road_connections.values().forEach(x->x.set_in_out_lanegroups());

        // store list of road connections in nodes
        for(common.RoadConnection rc : road_connections.values()) {
            if (rc.start_link.end_node != rc.end_link.start_node) {
                System.out.println("bad road connection: id=" + rc.getId()
                        + " start_link = " + rc.start_link.getId()
                        + " end_link = " + rc.end_link.getId()
                        + " start_link.end_node = " + rc.start_link.end_node.getId()
                        + " end_link.start_node = " + rc.end_link.start_node.getId() );
//                throw new OTMException("bad road connection: id=" + rc.getId()
//                        + " start_link.end_node = " + rc.start_link.end_node.getId()
//                        + " end_link.start_node = " + rc.end_link.start_node.getId() );
            }

            rc.start_link.end_node.add_road_connection(rc);
        }

        // populate link.outlink2lanegroups
        for(Link link : links.values()){

            // case sink
            if(link.is_sink)
                continue;

            // for each outlink, add all lanegroups from which outlink is reachable
            link.outlink2lanegroups = new HashMap<>();
            for(Long outlink_id : link.end_node.out_links.keySet()) {
                Set<AbstractLaneGroup> connected_lg = link.lanegroups.values().stream().filter(lg -> lg.is_link_reachable(outlink_id)).collect(toSet());
                if(!connected_lg.isEmpty())
                    link.outlink2lanegroups.put(outlink_id,connected_lg );
            }
        }

        // populate macro_internal_nodes: all nodes connect to macro links, minus source and sink nodes
        Set<Node> all_nodes = macro_link_models.stream().map(x->x.link.start_node).collect(toSet());
        all_nodes.addAll(macro_link_models.stream().map(x->x.link.end_node).collect(toSet()));
        all_nodes.removeAll(nodes.values().stream().filter(node->node.is_source || node.is_sink).collect(Collectors.toSet()));
        macro_internal_nodes = all_nodes;

        // give them models.ctm node models
        for(Node node : macro_internal_nodes)
            node.set_macro_model( new NodeModel(node) );

        // construct cells for macro links (this has to be after sources are set)
        if(!macro_link_models.isEmpty()){

            // ctm links
            if(model.getCtm()!=null)
                links.values().stream()
                        .filter(x->x.model_type==Link.ModelType.ctm)
                        .forEach(v->((models.ctm.LinkModel)v.model).create_cells(model.getCtm().getMaxCellLength()));

            // mn links
            if(model.getMn()!=null)
                links.values().stream()
                        .filter(x->x.model_type==Link.ModelType.mn)
                        .forEach(v->((models.ctm.LinkModel)v.model).create_cells(model.getMn().getMaxCellLength()));

        }

        // assign road params
        road_params = new HashMap<>();
        if(jaxb_params!=null)
            for(jaxb.Roadparam r : jaxb_params.getRoadparam())
                road_params.put(r.getId(),r);

        for( jaxb.Link jl : jaxb_links ) {
            Link link = links.get(jl.getId());
            jaxb.Roadparam rp = road_params.get(jl.getRoadparam());
            if(rp==null)
                throw new OTMException("No road parameters for link id " + jl.getId()  );
            link.model.set_road_param(rp,scenario.sim_dt);
        }

    }

    // constuctor for static scenario
    public Network(Scenario scenario,List<jaxb.Node> jaxb_nodes, List<jaxb.Link> jaxb_links, jaxb.Roadparams jaxb_params) throws OTMException {

        this(scenario);

        // read nodes
        for( jaxb.Node jn : jaxb_nodes ) {
            long id = jn.getId();
            if( nodes.containsKey(id) )
                throw new OTMException("Tried to add duplicate node id " + id);
            nodes.put(id,new Node(this,jn));
        }

        // create links
        macro_link_models = new HashSet<>();
        for( jaxb.Link jl : jaxb_links ) {
            long id = jl.getId();

            // check if we have the link id
            if( links.containsKey(id)  )
                throw new OTMException("Tried to add duplicate link id " + id );

            // get its type. if not specified, it is models.ctm.pq
            Link link = new Link(this,
                    null,
                    jl.getRoadparam() ,
                    null,
                    null ,
                    id,
                    jl.getLength(),
                    jl.getFullLanes(),
                    null,
                    nodes.get(jl.getStartNodeId()),
                    nodes.get(jl.getEndNodeId()) );

            models.ctm.LinkModel none_model = new models.ctm.LinkModel(link);
            link.set_model(none_model);

            links.put(id,link);

        }

        // assign road params
        road_params = new HashMap<>();
        if(jaxb_params!=null)
            for(jaxb.Roadparam r : jaxb_params.getRoadparam())
                road_params.put(r.getId(),r);

        for( jaxb.Link jl : jaxb_links ) {
            Link link = links.get(jl.getId());
            jaxb.Roadparam rp = road_params.get(jl.getRoadparam());
            if(rp==null)
                throw new OTMException("No road parameters for link id " + jl.getId()  );
            link.model.set_road_param(rp,scenario.sim_dt);
        }

    }

    public void validate(Scenario scenario,OTMErrorLog errorLog){
        nodes.values().forEach(x->x.validate(scenario,errorLog));
        links.values().forEach(x->x.validate(errorLog));
        road_geoms.values().forEach(x->x.validate(errorLog));
        road_connections.values().forEach(x->x.validate(errorLog));

        // special validation: if the network contains models.ctm links, then
        // all commodities must be pathfull. This is because we only have
        // a simplifies pathfull-only node model
//        if(!macro_link_models.isEmpty() && scenario.commodities.values().stream().map(x->x.pathfull).anyMatch(x->x==false))
//            errorLog.addWarning("Networks containing macroscopic links must have only pathfull commodities.");
    }

    public void initialize(Scenario scenario,RunParameters runParams) throws OTMException {

        for(Link link : links.values())
            link.initialize(scenario,runParams);

        for(Node node: nodes.values())
            node.initialize(scenario,runParams);
    }

    // This assumes that there are no upstream add_lanes.
    private Set<AbstractLaneGroup> create_lanegroups_from_roadconnections(Link link) throws OTMException {

        // validation TODO: does this belong here? or rather in someone's validation.
        if( link.road_geom!=null && (link.road_geom.up_left.lanes!=0 || link.road_geom.up_right.lanes!=0) )
            throw new OTMException(" link.road_geom.up_left.lanes!=0 || link.road_geom.up_right.lanes!=0 ");

        // get road connections that exit this link
        Set<RoadConnection> out_rcs = road_connections.values().stream().filter(x->x.start_link.id==link.id).collect(toSet());

        // empty out_rc is allowed only if num_out<=1
        if(out_rcs.isEmpty() && link.end_node.out_links.size()>1)
            throw new OTMException("No road connection leaving link " + link.getId() + ", although it is neither a sink nor a single next link case.");

        Set<AbstractLaneGroup> lanegroups = new HashSet<>();
        try {
            if(out_rcs.isEmpty()) { // sink or single next link

                // create a road connection if it is not a sink
                if (!link.is_sink) {
                    Long rc_id = ++max_rcid;
                    RoadConnection newrc = new RoadConnection(rc_id, link, link.end_node.out_links.values().iterator().next());
                    road_connections.put(rc_id, newrc);
                    out_rcs.add(newrc);
                }

                lanegroups.add(create_lane_group(link, null, out_rcs));
            }
            else {

                // create map from lanes to road connection sets
                Map<Integer,Set<RoadConnection>> lane2rcs = new HashMap<>();
                for(Integer lane : link.get_exit_lanes()){
                    Set<RoadConnection> myrcs = new HashSet<>();
                    for(RoadConnection rc : out_rcs)
                        if(rc.start_link_from_lane<=lane && rc.start_link_to_lane>=lane)
                            myrcs.add(rc);
                    lane2rcs.put(lane,myrcs);
                }

                // set of road connection sets
                Set<Set<RoadConnection>> unique_rc_sets = new HashSet<>();
                unique_rc_sets.addAll(lane2rcs.values());

                // create a lane group for each unique_rc_sets
                for(Set<RoadConnection> rc_set : unique_rc_sets){

                    // find which lanes are in this lanegroup
                    Set<Integer> lanes = new HashSet<>();
                    for(Integer lane : link.get_exit_lanes())
                        if(lane2rcs.get(lane).equals(rc_set))
                            lanes.add(lane);

                    // create the lanegroup
                    lanegroups.add(create_lane_group(link,lanes,rc_set));
                }
            }
        } catch (Exception e) {
            throw new OTMException(e);
        }

        return lanegroups;
    }

    // WARNING: this assumes no upstream addlanes.
    private AbstractLaneGroup create_lane_group(Link link,Set<Integer> lanes,Set<RoadConnection> out_rcs){

        assert(out_rcs!=null);

        if(lanes==null){ // include all lanes
            lanes = new HashSet<>();
            for(Integer lane : link.get_exit_lanes())
                lanes.add(lane);
        }

        AbstractLaneGroup lg = null;
        switch(link.model_type){
            case ctm:
            case mn:
                lg = new models.ctm.LaneGroup(link,lanes,out_rcs);
                break;
            case pq:
                lg = new models.pq.LaneGroup(link,lanes,out_rcs);
                break;
            case micro:
                lg = new models.micro.LaneGroup(link,lanes,out_rcs);
                break;
            case none:
                lg = new models.none.LaneGroup(link,lanes,out_rcs);
                break;
        }
        return lg;
    }

    ///////////////////////////////////////////
    // update
    ///////////////////////////////////////////

    public void update_macro_flow(float timestamp) throws OTMException {

        // lane changes
        // (cell.veh_in_target,cell.veh_notin_target -> cell.lane_change_veh)
//        macro_link_models.stream()
//                .filter(l -> l.link.lanegroups.size() >= 2)
//                .forEach(l -> l.update_lane_changes());

        // intermediate state update
        // (cell.lane_change_veh -> cell.veh_in_target,cell.veh_notin_target)
//        macro_link_models.stream()
//                .filter(l -> l.link.lanegroups.size()>=2)
//                .forEach(l -> l.intermediate_state_update());

        // update demand and supply
        // (cell.veh_in_target,cell.veh_notin_target -> cell.demand_in_target , cell.demand_notin_target)
        // (cell.veh_in_target,cell.veh_notin_target -> cell.supply)
        macro_link_models.forEach(l -> l.update_supply_demand());

        // compute node inflow and outflow (all nodes except sources)
        macro_internal_nodes.forEach(node->node.node_model.update_flow(timestamp));

        // exchange packets
        for(Node node : macro_internal_nodes) {

            // flows on road connections arrive to links on give lanes
            // convert to packets and send
            for(models.ctm.RoadConnection rc : node.node_model.rcs.values())
                rc.rc.end_link.model.add_vehicle_packet(timestamp,new PacketLink(rc.f_rs,rc.rc.out_lanegroups));

            // set exit flows on non-sink lanegroups
            for(UpLaneGroup ulg : node.node_model.ulgs.values())
                ulg.lg.release_vehicles(ulg.f_is);
        }

        // update cell boundary flows
        macro_link_models.forEach(l -> l.update_cell_boundary_flows());
        
    }

    public void update_macro_state(float timestamp) {
        for(models.ctm.LinkModel linkModel : macro_link_models)
            linkModel.update_state(timestamp);
    }

    ////////////////////////////////////////////
    // get / set
    ///////////////////////////////////////////

    // This is used only by otm-mpi to crop road connections from a base scenario into a new scenario
    public void set_roadconnections(Map<Long,RoadConnection> rcs){
        this.road_connections = rcs;
    }

    public Set<AbstractLaneGroup> get_lanegroups(){
        return links.values().stream().flatMap(link->link.lanegroups.values().stream()).collect(toSet());
    }

    public Collection<RoadConnection> get_road_connections(){
        return road_connections.values();
    }

    public RoadConnection get_road_connection(Long id){
        return road_connections.get(id);
    }

    @Override
    public String toString() {
        String str = "";
        str += "# nodes: " + nodes.size() + "\n";
        str += "# links: " + links.size() + "\n";
//        str += "# lanegroups: " + lanegroups.size() + "\n";
        str += "\n";
        for(Link link : links.values())
            str+="link "+link.id+":\n"+ link.toString();
        return str;
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
