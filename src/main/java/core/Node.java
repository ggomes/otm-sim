package core;

import actuator.AbstractActuator;
import error.OTMErrorLog;
import error.OTMException;
import actuator.InterfaceActuatorTarget;

import java.util.*;

public class Node implements InterfaceScenarioElement, InterfaceActuatorTarget {

    public Network network;
    protected final long id;
    protected Map<Long,Link> in_links;
    protected Set<Link> out_links;
    protected Set<RoadConnection> road_connections;

    protected boolean is_source;
    protected boolean is_sink;
    protected boolean is_many2one;

    // node flwpos
    protected Float xcoord;
    protected Float ycoord;

    protected AbstractActuator actuator;

    ///////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public Node(Network network,Long id,Float xcoord, Float ycoord, boolean is_vsource){
        this.network = network;
        this.id = id;
        this.xcoord = xcoord;
        this.ycoord = ycoord;

        this.in_links = new HashMap<>();
        this.out_links = new HashSet<>();
        this.road_connections = new HashSet<>();

        this.is_sink = true;
        this.is_source = true;
        this.is_many2one = false;
    }

    public Node(Network network,jaxb.Node jn){
        this(network,jn.getId(),jn.getX(),jn.getY(),jn.isVsource());
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
        return ScenarioElementType.node;
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
    }

    @Override
    public jaxb.Node to_jaxb(){
        jaxb.Node jnode = new jaxb.Node();
        jnode.setId(getId());
        jnode.setX(xcoord);
        jnode.setY(ycoord);
        return jnode;
    }

    public void add_road_connection(RoadConnection rc){
        this.road_connections.add(rc);
    }

    public void add_input_link(Link link){
        in_links.put(link.id,link);
        is_source = false;
    }

    public void add_output_link(Link link){
        out_links.add(link);
        is_sink = false;
    }

    ////////////////////////////////////////////
    //  InterfaceActuatorTarget
    ///////////////////////////////////////////

    @Override
    public String getTypeAsTarget() {
        return "node";
    }

    @Override
    public long getIdAsTarget() {
        return id;
    }

    @Override
    public void register_actuator(Set<Long> commids, AbstractActuator act) throws OTMException {
        if(actuator!=null)
            throw new OTMException("Node already has an actuator");
        this.actuator=act;
    }

    ////////////////////////////////////////////
    // API
    ///////////////////////////////////////////

    public Collection<Link> get_in_links(){
        return in_links.values();
    }

    public Collection<Link> get_out_links(){
        return out_links;
    }

    public int num_inputs(){
        return in_links.size();
    }

    public int num_outputs(){
        return out_links.size();
    }

    public Float get_x(){
        return xcoord;
    }

    public Float get_y(){
        return ycoord;
    }

    public Set<RoadConnection> get_road_connections(){
        return road_connections;
    }
}
