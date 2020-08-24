package common;

import actuator.AbstractActuator;
import commodity.Commodity;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import keys.KeyCommodityLink;
import profiles.SplitMatrixProfile;
import actuator.InterfaceActuatorTarget;

import java.util.*;

public class Node implements InterfaceActuatorTarget, InterfaceScenarioElement {

    public Network network;
    protected final long id;
    public Map<Long,Link> in_links;
    public Set<Link> out_links;
    public Set<RoadConnection> road_connections;

    public boolean is_source;
    public boolean is_vsource;
    public boolean is_sink;

    public boolean is_many2one;

    // split ratio data
    public Map<KeyCommodityLink, SplitMatrixProfile> splits;   // these hold split profiles and issue events when they change
                                                        // they are not used to retrieve split data. for this use the
                                                        // branders
    // actuator
    public AbstractActuator actuator;

    // node flwpos
    public Float xcoord;
    public Float ycoord;

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
        Scenario scenario = network.scenario;

        // all pathless commodities must have splits for all outlinks
//        if(out_links.size()>1){
//            for(Commodity comm : scenario.commodities.values()){
//                if(!comm.pathfull){
//                    for(Link inlink : in_links.values()){
//                        KeyCommodityLink key = new KeyCommodityLink(comm.getId(),inlink.id);
//                        if(splits==null || !splits.containsKey(key)){
//                            errorLog.addError(String.format("Node %d is missing split ratios for commodity %d from link %d",
//                                    id,comm.getId(),inlink.id));
//                            continue;
//                        }
////                        SplitMatrixProfile smp = splits.get(key);
////                        for(Link olink : out_links)
////                            if(!smp.has_split_for_outlink(olink.id))
////                                errorLog.addError(String.format("Node %d is missing split ratios for commodity %d from link %d to link %d",
////                                        id,comm.getId(),inlink.id,olink.id));
//                    }
//
//                }
//            }
//        }

        if(splits!=null){
            splits.values().stream().forEach(x -> x.validate(scenario,errorLog));
        }
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        if(splits!=null)
            for(SplitMatrixProfile x : splits.values())
                x.initialize(scenario.dispatcher.current_time);
    }

    @Override
    public void register_with_dispatcher(Dispatcher dispatcher) {

    }

    @Override
    public jaxb.Node to_jaxb(){
        jaxb.Node jnode = new jaxb.Node();
        jnode.setId(getId());
        jnode.setX(xcoord);
        jnode.setY(ycoord);
        return jnode;
    }

    ///////////////////////////////////////////
    // InterfaceActuatorTarget
    ///////////////////////////////////////////

    @Override
    public void register_actuator(AbstractActuator act) throws OTMException {
        if(this.actuator!=null)
            throw new OTMException("Multiple actuators on node");
        this.actuator = act;
    }

//    @Override
//    public long getIdAsTarget() {
//        return id;
//    }

    ///////////////////////////////////////////
    // XXX
    ///////////////////////////////////////////

    public void delete(){
        network = null;
        in_links = null;
        out_links = null;
        road_connections = null;
        splits = null;
        actuator = null;
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

    public void add_split(KeyCommodityLink key,SplitMatrixProfile smp){
        if(splits==null)
            splits = new HashMap<>();
        if(!splits.containsKey(key))
            splits.put(key,smp);
    }

    ////////////////////////////////////////////
    // get / set
    ///////////////////////////////////////////

    public void send_splits_to_inlinks(long commodity_id, long linkinid, Map<Long,Double> outlink2value) throws OTMException {
        Link linkin = in_links.  get(linkinid);
        if(linkin!=null)
           linkin.set_splits(commodity_id,outlink2value);
    }

    public int num_inputs(){
        return in_links.size();
    }

    public int num_outputs(){
        return out_links.size();
    }


}
