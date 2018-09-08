/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package common;

import actuator.AbstractActuator;
import commodity.Commodity;
import error.OTMErrorLog;
import error.OTMException;
import keys.KeyCommodityLink;
import models.ctm.NodeModel;
import profiles.SplitMatrixProfile;
import actuator.InterfaceActuatorTarget;
import runner.InterfaceScenarioElement;
import runner.RunParameters;
import runner.Scenario;
import runner.ScenarioElementType;

import java.util.*;

import static java.util.stream.Collectors.toSet;

public class Node implements InterfaceActuatorTarget, InterfaceScenarioElement {

    public Network network;
    protected final long id;
    public Map<Long,Link> in_links;
    public Map<Long,Link> out_links;
    public Set<Commodity> commodities;
    public Set<RoadConnection> road_connections;

    public boolean is_source;
    public boolean is_sink;

    public boolean is_many2one;

    // models.ctm model
    public boolean is_macro_node;  //
    public NodeModel node_model;

    // split ratio data
    public Map<KeyCommodityLink, SplitMatrixProfile> splits;   // these hold split profiles and issue events when they change
                                                        // they are not used to retrieve split data. for this use the
                                                        // branders
    // actuator
    public AbstractActuator actuator;

    // node position
    public Float xcoord;
    public Float ycoord;

    ////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public Node(Network network,Long id,Float xcoord, Float ycoord){
        this.network = network;
        this.id = id;
        this.xcoord = xcoord;
        this.ycoord = ycoord;

        this.is_macro_node = false;
        this.in_links = new HashMap<>();
        this.out_links = new HashMap<>();
        this.road_connections = new HashSet<>();

        this.is_sink = true;
        this.is_source = true;
        this.is_many2one = true;
    }

    public Node(Network network,jaxb.Node jn){
        this(network,jn.getId(),jn.getX(),jn.getY());
    }

    public void delete(){
        network = null;
        in_links = null;
        out_links = null;
        commodities = null;
        road_connections = null;
        node_model = null;
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
        out_links.put(link.id,link);
        is_sink = false;
    }

    public void add_split(KeyCommodityLink key,SplitMatrixProfile smp){
        if(splits==null)
            splits = new HashMap<>();
        if(!splits.containsKey(key))
            splits.put(key,smp);
    }

    public void set_commodities(){
        commodities = new HashSet<>();
        for(Link link : in_links.values())
            commodities.addAll(link.commodities);
        for(Link link : out_links.values())
            commodities.addAll(link.commodities);
    }

    public void set_macro_model(NodeModel model){
        this.is_macro_node = true;
        if( node_model==null )
            node_model = model;
    }

    @Override
    public void register_actuator(AbstractActuator act) throws OTMException {
        if(this.actuator!=null)
            throw new OTMException("Multiple actuators on node");
        this.actuator = act;
    }

    public void validate(Scenario scenario,OTMErrorLog errorLog){
        if(node_model!=null)
            node_model.validate(errorLog);

        if(splits!=null){
            splits.values().stream().forEach(x -> x.validate(scenario,errorLog));
        }
    }

    public void initialize(Scenario scenario, RunParameters runParams) throws OTMException {
        if(node_model!=null)
            node_model.initialize(scenario);

        if(splits!=null)
            for(SplitMatrixProfile x : splits.values())
                x.initialize(scenario.dispatcher.current_time);
    }

    ////////////////////////////////////////////
    // get / set
    ///////////////////////////////////////////

    public void set_node_split(long commodity_id, long linkinid, Map<Long,Double> outlink2value) throws OTMException {

        if(!commodities.stream().map(x->x.getId()).collect(toSet()).contains(commodity_id))
            throw new OTMException("Node " + getId() + "  does not support this commodity " + commodity_id);

        Link linkin = in_links.get(linkinid);
        linkin.packet_splitter.set_splits(commodity_id,outlink2value);
    }

    public int num_inputs(){
        return in_links.size();
    }

    public int num_outputs(){
        return out_links.size();
    }

    public jaxb.Node to_jaxb(){
        jaxb.Node jnode = new jaxb.Node();
        jnode.setId(getId());
        jnode.setX(xcoord);
        jnode.setY(ycoord);
        return jnode;
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
        return ScenarioElementType.node;
    }
}
