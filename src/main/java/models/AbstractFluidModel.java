package models;

import common.Link;
import common.Node;
import dispatch.*;
import error.OTMException;
import models.ctm.UpLaneGroup;
import packet.PacketLink;
import runner.Scenario;

import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public abstract class AbstractFluidModel extends AbstractModel {

    public float dt;
    private Set<NodeModel> node_models;

    public AbstractFluidModel(String name, boolean is_default, float dt) {
        super(name, is_default);
        this.dt = dt;
    }

    //////////////////////////////////////////////////////////////
    // load
    //////////////////////////////////////////////////////////////

    @Override
    public void set_links(Set<Link> links) {
        super.set_links(links);

        // populate macro_internal_nodes: connected in any way to ctm models, minus sources and sinks
        Set<Node> all_nodes = links.stream()
                .map(link->link.start_node)
                .filter(node->!node.is_source)
                .collect(toSet());

        all_nodes.addAll(links.stream()
                .map(link->link.end_node)
                .filter(node->!node.is_sink)
                .collect(toSet()));

        // give them models.ctm node models
        node_models = new HashSet<>();
        for(Node node : all_nodes)
            node_models.add( new NodeModel(node) );
    }

    @Override
    public void build() {

        // build node models
        node_models.forEach(m->m.build());
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);

        for(NodeModel node_model : node_models)
            node_model.initialize(scenario);
    }

    //////////////////////////////////////////////////////////////
    // run
    //////////////////////////////////////////////////////////////

    abstract public void update_flux_I(float timestamp) throws OTMException;
    abstract public void update_flux_II(float timestamp) throws OTMException;
    abstract public void update_link_state(Float timestamp,Link link);

    @Override
    public void register_with_dispatcher(Scenario scenario, Dispatcher dispatcher, float start_time){
        dispatcher.register_event(new EventFluidFluxUpdate(dispatcher, start_time + dt, this));
        dispatcher.register_event(new EventFluidStateUpdate(dispatcher, start_time + dt, this));
    }

    public void update_macro_state(Float timestamp) throws OTMException {
        for(Link link : links)
            update_link_state( timestamp, link);
    }

    public void update_macro_flow(Float timestamp) throws OTMException {

        update_flux_I(timestamp);

        // compute node inflow and outflow (all nodes except sources)
        node_models.forEach(n->n.update_flow(timestamp));

        // -- MPI communication (in otm-mpi) -- //

        // exchange packets
        for(NodeModel node_model : node_models) {

            // flows on road connections arrive to links on give lanes
            // convert to packets and send
            for(models.ctm.RoadConnection rc : node_model.rcs.values()) {
                Link link = rc.rc.get_end_link();
                link.model.add_vehicle_packet(link,timestamp, new PacketLink(rc.f_rs, rc.rc));
            }

            // set exit flows on non-sink lanegroups
            for(UpLaneGroup ulg : node_model.ulgs.values())
                ulg.lg.release_vehicles(ulg.f_is);
        }

        update_flux_II(timestamp);
    }

}
