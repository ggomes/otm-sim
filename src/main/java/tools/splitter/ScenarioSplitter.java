package tools.splitter;

import actuator.AbstractActuator;
import api.APIdev;
import common.Link;
import common.Node;
import control.AbstractController;
import error.OTMException;
import jaxb.AddLanes;
import jaxb.Split;
import keys.KeyCommodityDemandTypeId;
import keys.KeyCommodityLink;
import profiles.AbstractDemandProfile;
import profiles.DemandProfile;
import profiles.SplitMatrixProfile;
import runner.OTM;
import runner.Scenario;
import utils.OTMUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

public class ScenarioSplitter {

    public static APIdev api;
    public static Set<SplitNode> split_nodes;
    public static Set<Long> boundary_nodes = new HashSet<>();
    public static Set<Long> boundary_links = new HashSet<>();
    public static Set<Long> unplaced_nodes = new HashSet<>();
    public static Set<Long> unplaced_links = new HashSet<>();

    //////////////////////////////////////////////////
    public static void main(String [] args) {

//        Config cfg = load_a();
        Config cfg = load_large();

        // read the scenario
        try {
            api = new APIdev(OTM.load(cfg.config));

            to_metis(api.scenario,
                    "C:\\Users\\gomes\\code\\beats\\metisfile.txt",
                    "C:\\Users\\gomes\\code\\beats\\nodemap.txt");

//            split_nodes = cfg.split_nodes;
//
//            // split into two graphs (name->graph)
//            Map<String,Graph> graphs = split_into_graphs();
//
//            // graph->scenario and scenario->xml
//            for(Graph graph : graphs.values()){
//                jaxb.Scenario jsc = to_jaxb(graph,api.scenario());
//                jsc.setSensors(null);       // remove sensors, we don't care about sensors
//                to_xml(jsc,graph.name);
//            }

        } catch (OTMException e) {
            e.printStackTrace();
        }

    }
    private static Config load_a(){
        Config cfg = new Config();
        cfg.config = "C:\\Users\\gomes\\Dropbox\\gabriel\\work\\HPC\\split_network\\cfg\\a.xml";
        cfg.split_nodes = new HashSet<>();

        SplitNode s = new SplitNode(16L);
        s.add_links("A", Arrays.asList(16L));
        s.add_links("B", Arrays.asList(17L));
        cfg.split_nodes.add(s);

        return cfg;
    }
    private static Config load_large(){
        Config cfg = new Config();
        cfg.config = "C:\\Users\\gomes\\Dropbox\\gabriel\\work\\HPC\\split_network\\cfg\\beats_scenario_fixed.xml";
        cfg.split_nodes = new HashSet<>();

        SplitNode s;

        s = new SplitNode(8014400L);
        s.add_links("A", Arrays.asList(762L, 8014399L));
        s.add_links("B", Arrays.asList(8014396L, 776L));
        cfg.split_nodes.add(s);

        s = new SplitNode(21865L);
        s.add_links("A", Arrays.asList(8004830L, 21858L));
        s.add_links("B", Arrays.asList(21864L));
        cfg.split_nodes.add(s);

        s = new SplitNode(8009059L);
        s.add_links("A", Arrays.asList(8003958L));
        s.add_links("B", Arrays.asList(8009058L));
        cfg.split_nodes.add(s);

        s = new SplitNode(7633871L);
        s.add_links("A", Arrays.asList(7633918L));
        s.add_links("B", Arrays.asList(7633870L, 7633873L));
        cfg.split_nodes.add(s);

        s = new SplitNode(7633864L);
        s.add_links("A", Arrays.asList(7633863L));
        s.add_links("B", Arrays.asList(7633796L, 7633874L));
        cfg.split_nodes.add(s);

        s = new SplitNode(8008930L);
        s.add_links("A", Arrays.asList(8008929L));
        s.add_links("B", Arrays.asList(8008932L));
        cfg.split_nodes.add(s);

        s = new SplitNode(21861L);
        s.add_links("A", Arrays.asList(21831L, 21828L));
        s.add_links("B", Arrays.asList(21860L));
        cfg.split_nodes.add(s);

        s = new SplitNode(24821L);
        s.add_links("A", Arrays.asList(24823L, 411L));
        s.add_links("B", Arrays.asList(1124L, 24820L));
        cfg.split_nodes.add(s);

        s = new SplitNode(7994969L);
        s.add_links("A", Arrays.asList(516L, 7994971L));
        s.add_links("B", Arrays.asList(7994968L));
        cfg.split_nodes.add(s);

        s = new SplitNode(7994984L);
        s.add_links("A", Arrays.asList(7994983L));
        s.add_links("B", Arrays.asList(7994986L, 364L));
        cfg.split_nodes.add(s);

        s = new SplitNode(23614L);
        s.add_links("A", Arrays.asList(23616L, 366L));
        s.add_links("B", Arrays.asList(612L, 23613L));
        cfg.split_nodes.add(s);

        return cfg;
    }
    private static class Config{
        public String config;
        public Set<SplitNode> split_nodes;
    }
    //////////////////////////////////////////////////

    /////////////////////////////////////////////////////////////////////////////////////
    // private static
    /////////////////////////////////////////////////////////////////////////////////////

    // TODO: controllers, actuators
    private static jaxb.Scenario to_jaxb(Graph graph, Scenario base){

        jaxb.Scenario jsc = new jaxb.Scenario();

        // comodities
        jaxb.Commodities jcomms = new jaxb.Commodities();
        jsc.setCommodities(jcomms);
        for(commodity.Commodity comm : base.commodities.values()){
            jaxb.Commodity jcomm = new jaxb.Commodity();
            jcomms.getCommodity().add(jcomm);

            jcomm.setId(comm.getId());
            jcomm.setName(comm.name);
            jcomm.setPathfull(comm.pathfull);
            String str = OTMUtils.comma_format(comm.subnetworks.stream().map(x->x.getId()).collect(Collectors.toList()));
            jcomm.setSubnetworks(str);
        }

        // network
        jaxb.Network jnet = new jaxb.Network();
        jsc.setNetwork(jnet);

        // network: nodes
        jaxb.Nodes jnodes = new jaxb.Nodes();
        jnet.setNodes(jnodes);
        for(Long node_id : graph.nodes){
            common.Node node = base.network.nodes.get(node_id);
            jaxb.Node jnode = new jaxb.Node();
            jnodes.getNode().add(jnode);

            jnode.setId(node.getId());
            jnode.setX(node.xcoord);
            jnode.setY(node.ycoord);
        }

        // network: links
        jaxb.Links jlinks = new jaxb.Links();
        jnet.setLinks(jlinks);
        for(Long link_id : graph.links){
            common.Link link = base.network.links.get(link_id);
            jaxb.Link jlink = new jaxb.Link();
            jlinks.getLink().add(jlink);
            jlink.setId(link.getId());
            jlink.setStartNodeId(link.start_node.getId());
            jlink.setEndNodeId(link.end_node.getId());
            jlink.setLength(link.length);
            jlink.setFullLanes(link.full_lanes);
            if(link.road_geom!=null)
                jlink.setRoadgeom(link.road_geom.id);
            jlink.setRoadparam(0);
            jlink.setRoadType(link.road_type.toString());
            jaxb.Points jpoints = new jaxb.Points();
            jlink.setPoints(jpoints);
            for(common.Point point : link.shape){
                jaxb.Point jpoint = new jaxb.Point();
                jpoints.getPoint().add(jpoint);
                jpoint.setX(point.x);
                jpoint.setY(point.y);
            }
        }

        // network: roadgeoms
        jaxb.Roadgeoms jgeoms = new jaxb.Roadgeoms();
        jnet.setRoadgeoms(jgeoms);
        for(geometry.RoadGeometry geom : base.network.road_geoms.values()){
            jaxb.Roadgeom jgeom = new jaxb.Roadgeom();
            jgeoms.getRoadgeom().add(jgeom);
            jgeom.setId(geom.id);
            List<AddLanes> jaddlanes = jgeom.getAddLanes();
            if(geom.up_in.lanes>0)
                jaddlanes.add(geom.up_in.to_jaxb());
            if(geom.dn_in.lanes>0)
                jaddlanes.add(geom.dn_in.to_jaxb());
            if(geom.up_out.lanes>0)
                jaddlanes.add(geom.up_out.to_jaxb());
            if(geom.dn_out.lanes>0)
                jaddlanes.add(geom.dn_out.to_jaxb());
        }

        // network: roadconnections
        jaxb.Roadconnections jconns = new jaxb.Roadconnections();
        jnet.setRoadconnections(jconns);
        for(common.RoadConnection rcn : base.network.get_road_connections()){

            Long start_link = rcn.get_start_link_id();
            Long end_link = rcn.get_end_link_id();

            if( graph.links.contains(start_link) && graph.links.contains(end_link) ){
                jaxb.Roadconnection jrcn = new jaxb.Roadconnection();
                jconns.getRoadconnection().add(jrcn);
                jrcn.setId(rcn.getId());
                jrcn.setInLink(start_link);
                jrcn.setInLinkLanes(rcn.start_link_from_lane + "#" + rcn.start_link_to_lane);
//            jrcn.setLength(rcn);
                jrcn.setOutLink(end_link);
                jrcn.setOutLinkLanes(rcn.end_link_from_lane + "#" + rcn.end_link_to_lane);
            }
        }

        // network: roadparams
        jaxb.Roadparams jrpms = new jaxb.Roadparams();
        jnet.setRoadparams(jrpms);
        jrpms.getRoadparam().addAll(base.network.road_params.values());

        // subnetworks
        jaxb.Subnetworks jsubs = new jaxb.Subnetworks();
        jsc.setSubnetworks(jsubs);
        for(commodity.Subnetwork subnetwork : base.subnetworks.values()){
            jaxb.Subnetwork jsub = new jaxb.Subnetwork();
            jsubs.getSubnetwork().add(jsub);
            jsub.setId(subnetwork.getId());
            jsub.setName(subnetwork.getName());

            List<Long> subnet = subnetwork.get_link_ids();
            subnet.retainAll(graph.links);
            jsub.setContent(OTMUtils.comma_format(subnet));
        }

        // demands
        jaxb.Demands jdems = new jaxb.Demands();
        jsc.setDemands(jdems);
        for(Map.Entry<KeyCommodityDemandTypeId, AbstractDemandProfile> e : base.data_demands.entrySet()){
            KeyCommodityDemandTypeId key = e.getKey();
            DemandProfile demand = (DemandProfile) e.getValue();
            commodity.Commodity comm = base.commodities.get(key.commodity_id);
            // check whether the demand is into this graph
            if(graph.links.contains(demand.get_origin().getId())){
                jaxb.Demand jdem = new jaxb.Demand();
                jdems.getDemand().add(jdem);

                if(comm.pathfull)
                    jdem.setSubnetwork(demand.path.getId());
                else
                    jdem.setLinkId(demand.link.getId());

                jdem.setContent(OTMUtils.comma_format(demand.profile.values));
                jdem.setDt(demand.profile.dt);
                jdem.setCommodityId(comm.getId());
                jdem.setStartTime(demand.profile.start_time);
            }
        }

        // splits
        jaxb.Splits jsplits = new jaxb.Splits();
        jsc.setSplits(jsplits);
        for(Long node_id : graph.nodes){
            common.Node node = base.network.nodes.get(node_id);
            if(node.splits!=null && !node.splits.isEmpty()) {
                for(Map.Entry<KeyCommodityLink, SplitMatrixProfile> e : node.splits.entrySet()){
                    KeyCommodityLink key = e.getKey();
                    SplitMatrixProfile profile = e.getValue();

                    jaxb.SplitNode jspltnode = new jaxb.SplitNode();
                    jsplits.getSplitNode().add(jspltnode);

                    jspltnode.setCommodityId(key.commodity_id);
                    jspltnode.setDt(profile.get_dt());
                    jspltnode.setStartTime(profile.get_start_time());
                    jspltnode.setLinkIn(profile.link_in_id);
                    jspltnode.setNodeId(node_id);

                    List<Split> splitlist = jspltnode.getSplit();
                    for(Map.Entry<Long,List<Double>> e1 : profile.get_outlink_to_profile().entrySet()){
                        jaxb.Split split = new jaxb.Split();
                        splitlist.add(split);
                        split.setLinkOut(e1.getKey());
                        split.setContent( OTMUtils.comma_format(e1.getValue()));
                    }
                }
            }
        }

        // controllers
        jaxb.Controllers jctrls = new jaxb.Controllers();
        jsc.setControllers(jctrls);
        for(AbstractController absctrl : base.controllers.values()){
            jaxb.Controller jctrl = new jaxb.Controller();
            jctrls.getController().add(jctrl);

            jctrl.setId(absctrl.id);
            jctrl.setType(absctrl.type.toString());

            jctrl.setDt(absctrl.dt);
//            jctrl.setSchedule();

//            List<Long> acts = absctrl.actuators.stream().map(x->x.id).collect(Collectors.toList());
//            jctrl.setTargetActuators(OTMUtils.comma_format(acts));
        }


        // actuators
        jaxb.Actuators jacts = new jaxb.Actuators();
        jsc.setActuators(jacts);
        for(AbstractActuator absact : base.actuators.values()){
            jaxb.Actuator jact = new jaxb.Actuator();
            jacts.getActuator().add(jact);

            jact.setId(absact.id);
//            jact.setActuatorTarget(absact.get_target());
//            jact.setSignal();
            jact.setType(absact.getType().toString());
        }

        // sensors
//        jaxb.Sensors jsnsrs = new jaxb.Sensors();
//        jsc.setSensors(jsnsrs);
//        for(AbstractSensor abssns : base.sensors.values()){
//            Long link_id = abssns.link.getId();
//            if(graph.links.contains(link_id)) {
//                jaxb.Sensor jsns = new jaxb.Sensor();
//                jsnsrs.getSensor().add(jsns);
//                jsns.setId(abssns.id);
//                jsns.setDt(abssns.dt);
////            jsns.setLength(abssns.);
//                jsns.setLinkId(link_id);
////            jsns.setDataId(abssns);
//                jsns.setLanes(abssns.start_lane + "#" + abssns.end_lane);
//                jsns.setPosition(abssns.flwdir);
//                jsns.setType(abssns.type.toString());
//            }
//        }

        return jsc;

    }

    private static void to_xml(jaxb.Scenario jsc,String name){

        try {

            File file = new File("C:\\Users\\gomes\\code\\beats\\" + name + ".xml");
            JAXBContext jaxbContext = JAXBContext.newInstance(jaxb.Scenario.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // output pretty printed
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            jaxbMarshaller.marshal(jsc, file);
//            jaxbMarshaller.marshal(jsc, System.out);

        } catch (JAXBException e) {
            e.printStackTrace();
        }

    }

    private static void to_metis(Scenario scenario,String filename,String nodefilename){

        int num_nodes = scenario.network.nodes.size();

        // replace node ids with indices
        List<Node> node_list = new ArrayList<>();
        Map<Long,Integer> node_id2index = new HashMap<>();
        int i=0;
        for(Node node : scenario.network.nodes.values()){
            node_list.add(node);
            node_id2index.put(node.getId(),i+1);
            i++;
        }

        // create set of undirected edges
        Set<UEdge> edges = new HashSet<>();
        for(Link link : scenario.network.links.values()){
            int start_index = node_id2index.get(link.start_node.getId());
            int end_index = node_id2index.get(link.end_node.getId());
            edges.add(new UEdge(start_index,end_index));
        }
        int num_uedges = edges.size();

        // write to metis format
        try {
            PrintWriter writer = new PrintWriter(filename, "UTF-8");
            writer.println( num_nodes + " " + num_uedges );
            for(Node node : node_list){
                Integer node_index = node_id2index.get(node.getId());
                Set<UEdge> myedges = edges.stream()
                        .filter(edge->edge.u==node_index || edge.v==node_index)
                        .collect(Collectors.toSet());
                Set<Integer> neigbors = myedges.stream()
                        .flatMap(edge->edge.get_nodes().stream())
                        .collect(Collectors.toSet());
                neigbors.remove(node_index);
                writer.println(OTMUtils.format_delim(neigbors.toArray()," "));
            }
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // write node to index map
        try {
            PrintWriter writer = new PrintWriter(nodefilename, "UTF-8");
            for(Map.Entry<Long,Integer> e : node_id2index.entrySet())
                writer.println(e.getKey()+","+e.getValue());
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    /////////////////////////////////////////////////////////////////////////////////////
    // private
    /////////////////////////////////////////////////////////////////////////////////////

    private static Map<String,Graph> split_into_graphs() throws OTMException {

        Map<String, Graph> graphs = new HashMap<>();

        // only boundary nodes have been placed

        // all nodes and links start out unplaced
        unplaced_nodes.addAll(api.scenario.network.nodes.keySet());
        unplaced_links.addAll(api.scenario.network.links.keySet());

        // initialize graphs with split nodes and adjacent graph2links
        graphs.put("A", new Graph("A"));
        graphs.put("B", new Graph("B"));

        // store boundary nodes and links
        for (SplitNode s : split_nodes) {
            for (Graph g : graphs.values()) {
                g.add_node(s.node_id);
                g.add_links(s.get_links(g.name));
                boundary_links.addAll(s.get_links(g.name));
            }
            boundary_nodes.add(s.node_id);
        }

        // remove what has already been assigned
//        graphs.values().forEach(g -> unplaced_links.removeAll(g.links));

        // iterate split nodes.
        // Grow networks from each splitnode link into each network
        for (SplitNode splitNode : split_nodes) {
            for (Graph g : graphs.values()) {
                for (Long link : splitNode.get_links(g.name)) {
                    Long internal_link = get_internal_link(link, splitNode, g);
                    g.add_link(internal_link);
                    grow_graph(internal_link, g);
                    if (unplaced_links.isEmpty()) break;
                }
                if (unplaced_links.isEmpty()) break;
            }
            if (unplaced_links.isEmpty()) break;
        }

        Set<Long> unplaced_nodes_clone = new HashSet<>();
        unplaced_nodes_clone.addAll(unplaced_nodes);

        for (Long node_id : unplaced_nodes_clone) {

            // links adjacent to the node
            Node node = api.scenario.network.nodes.get(node_id);
            Set<Long> adj_links = new HashSet<>();
            adj_links.addAll(node.in_links.keySet());
            adj_links.addAll(node.out_links.keySet());

            // graphs containing all of these links
            Set<Graph> in_graphs = graphs.values().stream().filter(g -> g.links.containsAll(adj_links)).collect(Collectors.toSet());

            if (in_graphs.size() != 1) {
                System.err.println("What happened here?");
                continue;
            }

            in_graphs.iterator().next().add_node(node_id);

        }

        if (!unplaced_nodes.isEmpty() || !unplaced_links.isEmpty())
            System.err.println("Something bad.");

        // validate the graphs
        for(Graph graph : graphs.values())
            graph.validate();

        return graphs;
    }

    private static void grow_graph(Long curr_link_id, Graph graph) {

        if (boundary_links.contains(curr_link_id))
            return;

        // get start and end nodes
        Link curr_link = api.scenario.network.links.get(curr_link_id);
        Node start_node = curr_link.start_node;
        Node end_node = curr_link.end_node;

        // collect all attached graph2links.
        Set<Long> add_links = new HashSet<>();

        // add all incident on start node
        add_links.addAll(start_node.out_links.keySet());
        add_links.addAll(start_node.in_links.keySet());

        // add all incident on end node
        add_links.addAll(end_node.out_links.keySet());
        add_links.addAll(end_node.in_links.keySet());

        // keep only unplaced graph2links
        add_links.remove(curr_link_id);
        add_links.retainAll(unplaced_links);

        if (!add_links.isEmpty()) {

            // add all of these to the graph
            graph.add_links(add_links);

            // recursive call
            add_links.forEach(link -> grow_graph(link, graph));
        }
    }

    private static Long get_internal_link(Long blink_id, SplitNode splitNode, Graph g) {

        Link blink = api.scenario.network.links.get(blink_id);
        Long start_node = blink.start_node.getId();
        Long end_node = blink.end_node.getId();

        Long internal_link = null;
        if (splitNode.node_id == start_node && !blink.end_node.out_links.isEmpty()) {
            internal_link = blink.end_node.out_links.values().iterator().next().getId();
        } else if (splitNode.node_id == end_node && !blink.start_node.in_links.isEmpty()) {
            internal_link = blink.start_node.in_links.values().iterator().next().getId();
        }
        return internal_link;
    }

    /////////////////////////////////////////////////////////////////////////////////////
    // classes
    /////////////////////////////////////////////////////////////////////////////////////

    public static class Graph {
        public String name;
        public Set<Long> nodes = new HashSet<>();
        public List<Long> links = new ArrayList<>();

        public Graph(String name) {
            this.name = name;
        }

        public void add_link(Long x) {
            this.links.add(x);
            unplaced_links.remove(x);
        }

        public void add_links(Set<Long> x) {
            this.links.addAll(x);
            unplaced_links.removeAll(x);
        }

        public void add_node(Long x) {
            this.nodes.add(x);
            unplaced_nodes.remove(x);
        }

        public void add_nodes(Set<Long> x) {
            this.nodes.addAll(x);
            unplaced_nodes.removeAll(x);
        }

        public void validate() throws OTMException {
            // no repeated links

            Set<Long> link_unique = new HashSet<>(links);
            if(link_unique.size() < links.size())
                throw new OTMException("Duplicate links (" + (links.size()-link_unique.size()) +")");

            // no repeated nodes
            Set<Long> node_unique = new HashSet<>(nodes);
            if(node_unique.size() < nodes.size())
                throw new OTMException("Duplicate nodes (" + (nodes.size()-node_unique.size()) +")");
        }
    }

    public static class UEdge {
        public int u;
        public int v;
        public UEdge(int a,int b){
            this.u = Math.min(a,b) ;
            this.v = Math.max(a,b);
        }

        public Set<Integer> get_nodes(){
            Set<Integer> nodes = new HashSet<>();
            nodes.add(u);
            nodes.add(v);
            return nodes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UEdge uEdge = (UEdge) o;
            return u == uEdge.u &&
                    v == uEdge.v;
        }

        @Override
        public int hashCode() {
            return Objects.hash(u, v);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////
    // extra code
    /////////////////////////////////////////////////////////////////////////////////////


//    private static Set<SplitNode> read_split_nodes_json(String split_file) {
//        Set<SplitNode> split_nodes = new HashSet<>();
//
//
//        try {
//            JsonReader reader = new JsonReader(new FileReader(split_file));
//
//            reader.beginArray();
//
//            while (reader.hasNext()) {
//
//                reader.beginObject();
//                System.out.println(reader.nextName());
//                System.out.println(reader.nextString());
//
//                System.out.println(reader.nextName());
//
//                reader.beginObject();
//                System.out.println(reader.nextName());
//
//                reader.beginArray();
//                System.out.println(reader.nextString());
//                reader.endArray();
//
//                reader.endObject();
//
//                reader.endObject();
//
////                if (name.equals("name")) {
////
////                    System.out.println(reader.nextString());
////
////                } else if (name.equals("age")) {
////
////                    System.out.println(reader.nextInt());
////
////                } else if (name.equals("message")) {
////
////                    // read array
////                    reader.beginArray();
////
////                    while (reader.hasNext()) {
////                        System.out.println(reader.nextString());
////                    }
////
////                    reader.endArray();
////
////                } else {
////                    reader.skipValue(); //avoid some unhandle events
////                }
//            }
//
//            reader.endArray();
//            reader.close();
//
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//
////
////
////        Gson g = new Gson();
////
////        Person person = g.fromJson("{\"name\": \"John\"}", Person.class);
////        System.out.println(person.name); //John
////
////        System.out.println(g.toJson(person)); // {"name":"John"}
//        return split_nodes;
//    }


//    private static Set<SplitNode> read_split_nodes_csv(String split_file){
//        Set<SplitNode> split_nodes = new HashSet<>();
//        String line;
//        try (BufferedReader br = new BufferedReader(new FileReader(split_file))) {
//            while ((line = br.readLine()) != null) {
//                String[] x = line.split(",");
//                split_nodes.add(new SplitNode(Long.parseLong(x[0]),Double.parseDouble(x[1])));
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return split_nodes;
//    }

}
