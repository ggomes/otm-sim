package common;

import actuator.AbstractActuator;
import error.OTMErrorLog;
import error.OTMException;
import actuator.InterfaceActuatorTarget;

import java.util.*;

public class Node implements InterfaceScenarioElement, InterfaceActuatorTarget {

    public Network network;
    protected final long id;
    public Map<Long,Link> in_links;
    public Set<Link> out_links;
    public Set<RoadConnection> road_connections;

    public boolean is_source;
    public boolean is_vsource;
    public boolean is_sink;

    public boolean is_many2one;

    // node flwpos
    public Float xcoord;
    public Float ycoord;

    public AbstractActuator actuator;

    ///////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public Node(Network network,Long id,Float xcoord, Float ycoord, boolean is_vsource){
        this.network = network;
        this.id = id;
        this.xcoord = xcoord;
        this.ycoord = ycoord;
        this.is_vsource = is_vsource;

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
    public void initialize(Scenario scenario) throws OTMException {

    }

    @Override
    public jaxb.Node to_jaxb(){
        jaxb.Node jnode = new jaxb.Node();
        jnode.setId(getId());
        jnode.setX(xcoord);
        jnode.setY(ycoord);
        return jnode;
    }

    public void delete(){
        network = null;
        in_links = null;
        out_links = null;
        road_connections = null;
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
    // get / set
    ///////////////////////////////////////////

    public int num_inputs(){
        return in_links.size();
    }

    public int num_outputs(){
        return out_links.size();
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

}
