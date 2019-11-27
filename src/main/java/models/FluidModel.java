package models;

import common.AbstractSource;
import common.Link;
import common.Node;
import dispatch.*;
import error.OTMException;
import keys.KeyCommPathOrLink;
import models.ctm.Cell;
import models.ctm.FluidSource;
import models.ctm.UpLaneGroup;
import packet.PacketLink;
import runner.Scenario;
import utils.StochasticProcess;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public abstract class FluidModel extends BaseModel {

    public float dt;
    private Set<Link> source_links;
    private Set<Link> sink_links;
    private Map<Long,NodeModel> node_models;

    public FluidModel(String name, boolean is_default, float dt, StochasticProcess process) {
        super(name, is_default,process);
        this.dt = dt;
    }

    //////////////////////////////////////////////////////////////
    // load
    //////////////////////////////////////////////////////////////

    @Override
    public void set_links(Set<Link> links) {
        super.set_links(links);

        source_links = links.stream().filter(link->link.is_source).collect(toSet());
        sink_links = links.stream().filter(link->link.is_sink).collect(toSet());

        // create node models for all nodes in this model, except terminal nodes
        Set<Node> all_nodes = links.stream()
                .map(link->link.start_node)
                .filter(node->!node.is_source)
                .collect(toSet());

        all_nodes.addAll(links.stream()
                .map(link->link.end_node)
                .filter(node->!node.is_sink)
                .collect(toSet()));

        node_models = new HashMap<>();
        for(Node node : all_nodes) {
            NodeModel nm = new NodeModel(node);
            node_models.put(node.getId(),nm);
//            node.set_node_model(nm);
        }
    }

    @Override
    public void build() {

        // build node models
        node_models.values().forEach(m->m.build());
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);

        for(NodeModel node_model : node_models.values())
            node_model.initialize(scenario);
    }

    //////////////////////////////////////////////////////////////
    // run
    //////////////////////////////////////////////////////////////

    // udpate supplies and demands
    abstract public void update_link_flux_part_I(Link link, float timestamp) throws OTMException;
    abstract public void update_link_state(Link link,float timestamp) throws OTMException;

    @Override
    public void register_with_dispatcher(Scenario scenario, Dispatcher dispatcher, float start_time){
        dispatcher.register_event(new EventFluidFluxUpdate(dispatcher, start_time + dt, this));
        dispatcher.register_event(new EventFluidStateUpdate(dispatcher, start_time + dt, this));
    }

    public void update_fluid_flux(float timestamp) throws OTMException {

        update_fluid_flux_part_I(timestamp);

        // -- MPI communication (in otm-mpi) -- //

        update_fluid_flux_part_II(timestamp);

    }

    // update supplies and demands, then run node model to obtain inter-link flows
    public void update_fluid_flux_part_I(float timestamp) throws OTMException {

        // lane changes and compute demand and supply
        for(Link link : links)
            update_link_flux_part_I(link,timestamp);

        // compute node inflow and outflow (all nodes except sources)
        node_models.values().forEach(n->n.update_flow(timestamp));

    }

    // compute source and source flows
    // node model exchange packets
    public void update_fluid_flux_part_II(float timestamp) throws OTMException {


        // add to source links
        for(Link link : source_links){

            for(AbstractSource asource : link.sources){
                FluidSource source = (FluidSource) asource;
                for(Map.Entry<Long,Map<KeyCommPathOrLink,Double>> e : source.source_flows.entrySet()){
                    models.ctm.LaneGroup lg = (models.ctm.LaneGroup) link.lanegroups_flwdn.get(e.getKey());
                    Cell upcell = lg.cells.get(0);
                    upcell.add_vehicles(e.getValue(),null,null);
                }
            }
        }

        // release from sink links
        for(Link link : sink_links){

            for(BaseLaneGroup alg : link.lanegroups_flwdn.values()) {
                models.ctm.LaneGroup lg = (models.ctm.LaneGroup) alg;
                Map<KeyCommPathOrLink,Double> flow_dwn = lg.get_dnstream_cell().demand_dwn;

                lg.release_vehicles(flow_dwn);

                for(Map.Entry<KeyCommPathOrLink,Double> e : flow_dwn.entrySet())
                    if(e.getValue()>0)
                        lg.update_flow_accummulators(e.getKey(),e.getValue());
            }

        }


        // node models exchange packets
        for(NodeModel node_model : node_models.values()) {

            // flows on road connections arrive to links on give lanes convert to packets and send
            for(models.ctm.RoadConnection rc : node_model.rcs.values()) {
                Link link = rc.rc.get_end_link();
                link.model.add_vehicle_packet(link,timestamp, new PacketLink(rc.f_rs, rc.rc));
            }

            // set exit flows on non-sink lanegroups
            for(UpLaneGroup ulg : node_model.ulgs.values()) {
                ulg.lg.release_vehicles(ulg.f_gs);

                // send lanegroup exit flow to flow accumulator
                for(Map.Entry<KeyCommPathOrLink,Double> e : ulg.f_gs.entrySet())
                    if(e.getValue()>0)
                        ulg.lg.update_flow_accummulators(e.getKey(),e.getValue());
            }

        }

    }

    // intra link flows and states
    public void update_fluid_state(float timestamp) throws OTMException {
        for(Link link : links)
            update_link_state(link,timestamp);
    }

    //////////////////////////////////////////////////////////////
    // getters
    //////////////////////////////////////////////////////////////

    public NodeModel get_node_model_for_node(Long node_id){
        return node_models.get(node_id);
    }

}
