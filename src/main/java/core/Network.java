package core;

import commodity.Commodity;
import error.OTMException;
import core.geometry.*;

import java.util.*;

import static java.util.stream.Collectors.toSet;

/** Container for the network.
 * This class provides access to the links, nodes, and road connections in the network.
 */
public class Network {

    protected static Long max_rcid;

    protected Scenario scenario;
    protected boolean node_positions_in_meters;    // true->meters, false->gps

    protected Map<Long,jaxb.Roadparam> road_params;    // keep this for the sake of the scenario splitter
    protected Map<Long, RoadGeometry> road_geoms;

    // scenario elements
    /** Map of nodes **/
    public Map<Long,Node> nodes;

    /** Map of links **/
    public Map<Long,Link> links;

    /** Map of road connections **/
    public Map<Long,RoadConnection> road_connections;

    ///////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public Network(Scenario scenario){
        this.scenario = scenario;
        nodes = new HashMap<>();
        links = new HashMap<>();
    }

    public Network(Scenario scenario, jaxb.Nodes jaxb_nodes, List<jaxb.Link> jaxb_links, jaxb.Roadgeoms jaxb_geoms, jaxb.Roadconnections jaxb_conns, jaxb.Roadparams jaxb_params) throws OTMException {

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
        for(RoadConnection rc : road_connections.values()) {
            if(rc.start_link!=null){
                rc.start_link.end_node.add_road_connection(rc);
            } else if(rc.end_link!=null){
                rc.end_link.start_node.add_road_connection(rc);
            } else {
                System.err.println("bad road connection: id=" + rc.getId());
            }
        }

    }

    /////////////////////////////////////////////////
    // InterfaceScenarioElement-like
    /////////////////////////////////////////////////

    protected void initialize(Scenario scenario,float start_time) throws OTMException {
        for(Link link : links.values())
            link.initialize(scenario,start_time);
    }

    protected jaxb.Network to_jaxb(){
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
        for(core.geometry.RoadGeometry geom : road_geoms.values())
            jgeoms.getRoadgeom().add(geom.to_jaxb());

        // network: roadconnections
        jaxb.Roadconnections jconns = new jaxb.Roadconnections();
        jnet.setRoadconnections(jconns);
        for(core.RoadConnection rcn : road_connections.values())
            jconns.getRoadconnection().add(rcn.to_jaxb());

        // network: roadparams
        jaxb.Roadparams jrpms = new jaxb.Roadparams();
        jnet.setRoadparams(jrpms);
        jrpms.getRoadparam().addAll(road_params.values());

        return jnet;
    }

    /////////////////////////////////////////////////
    // API
    /////////////////////////////////////////////////


    /** Get map of nodes **/
    public Map<Long,Node> get_nodes(){
        return nodes;
    }

    /** Get map of links **/
    public Map<Long, Link> get_links(){
        return links;
    }

    /** Get map of road connections **/
    public Map<Long, RoadConnection> get_roadconnections(){
        return road_connections;
    }

    // id sets .......................................

    /** Get node ids **/
    public Set<Long> node_ids(){
        return nodes.keySet();
    }

    /** Get  ids **/
    public Set<Long> link_ids(){
        return links.keySet();
    }

    /** Get source link  ids **/
    public Set<Long> source_link_ids(){
        return links.values().stream()
                .filter(x->x.is_source())
                .map(x->x.getId())
                .collect(toSet());
    }


    // get element by id ..............................

    /** Get node by id **/
    public Node get_node(long id)throws OTMException {
        if(!nodes.containsKey(id))
            throw new OTMException("Bad id in Scenario.get_link");
        return nodes.get(id);
    }

    /** Get link by id **/
    public Link get_link(long id) throws OTMException {
        if(!links.containsKey(id))
            throw new OTMException("Bad id in Scenario.get_link");
        return links.get(id);
    }


    public Set<List<Long>> get_link_connectivity(){
        Set<List<Long>> X = new HashSet<>();
        for(Link link : links.values()){
            List<Long> A = new ArrayList<>();
            A.add(link.getId());
            A.add(link.get_start_node().getId());
            A.add(link.get_end_node().getId());
            X.add(A);
        }
        return X;
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
                    jl.getPoints(),
                    jl.getAltNextLink() );

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

    ///////////////////////////////////////////
    // toString
    ///////////////////////////////////////////

    @Override
    public String toString() {
        return String.format("%d nodes, %d links",nodes.size(),links.size());
    }

}
