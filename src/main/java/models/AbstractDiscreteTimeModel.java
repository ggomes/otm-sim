package models;

import commodity.Commodity;
import commodity.Path;
import commodity.Subnetwork;
import common.Link;
import common.Node;
import dispatch.Dispatcher;
import dispatch.EventMacroFlowUpdate;
import dispatch.EventMacroStateUpdate;
import error.OTMException;
import models.ctm.LaneGroup;
import models.ctm.UpLaneGroup;
import packet.PacketLink;
import runner.Scenario;
import utils.OTMUtils;

import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public abstract class AbstractDiscreteTimeModel extends AbstractModel {

    public float dt;
    public float max_cell_length;
    public Set<MacroNodeModel> node_models;

    // link model
    abstract public void update_state(float timestamp,Link link);
    abstract public void update_dwn_flow(Link link);
    abstract public void update_supply_demand(Link link);
    abstract public void perform_lane_changes(Link link,float timestamp);


    public AbstractDiscreteTimeModel(Set<Link> links,String name,boolean is_default,Float dt,Float max_cell_length) {
        super(links,name,is_default);
        this.model_type = ModelType.discrete_time;
        this.dt = dt==null ? -1 : dt;
        this.max_cell_length = max_cell_length==null ? -1 : max_cell_length;

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
            node_models.add( new MacroNodeModel(node) );
    }

    @Override
    public void build(Link link) {
        create_cells(link,max_cell_length);
    }

    @Override
    public void register_first_events(Scenario scenario, Dispatcher dispatcher,float start_time) {
        dispatcher.register_event(new EventMacroFlowUpdate(dispatcher, start_time + dt, this));
        dispatcher.register_event(new EventMacroStateUpdate(dispatcher, start_time + dt, this));
    }

    @Override
    public void register_commodity(Link link,Commodity comm, Subnetwork subnet) throws OTMException {

        if(comm.pathfull) {
            Link next_link = ((Path) subnet).get_link_following(link);
            Long next_link_id = next_link==null ? null : next_link.getId();
            for (AbstractLaneGroup lg : link.lanegroups_flwdn.values())
                lg.add_state(comm.getId(), subnet.getId(),next_link_id, true);
        }

        else {

            // for pathless/sink, next link id is same as this id
            if (link.is_sink) {
                for (AbstractLaneGroup lg : link.lanegroups_flwdn.values())
                    lg.add_state(comm.getId(), null,link.getId(), false);

            } else {

                // for pathless non-sink, add a state for each next link in the subnetwork
                for( Long next_link_id : link.outlink2lanegroups.keySet()  ){
                    if (!subnet.has_link_id(next_link_id))
                        continue;
                    for (AbstractLaneGroup lg : link.lanegroups_flwdn.values())
                        lg.add_state(comm.getId(), null,next_link_id, false);
                }
            }
        }

    }

    private void create_cells(Link link,float max_cell_length){

        // construct cells

        // create cells
        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values()) {

            float r = lg.length/max_cell_length;
            boolean is_source_or_sink = link.is_source || link.is_sink;

            int cells_per_lanegroup = is_source_or_sink ?
                    1 :
                    OTMUtils.approximately_equals(r%1.0,0.0) ? (int) r :  1+((int) r);
            float cell_length_meters = is_source_or_sink ?
                    lg.length :
                    lg.length/cells_per_lanegroup;

            ((LaneGroup) lg).create_cells(cells_per_lanegroup, cell_length_meters);
        }
    }

    ///////////////////////////////////////////
    // update
    ///////////////////////////////////////////

    public void update_macro_flow(float timestamp) throws OTMException {

        update_macro_flow_part_I(timestamp);

        // -- MPI communication (in otm-mpi) -- //

        update_macro_flow_part_II(timestamp);
    }

    public void update_macro_state(float timestamp) {
        for(Link link : links)
            update_state(timestamp,link);
    }

    public void update_macro_flow_part_I(float timestamp){

        // lane changing -> intermediate state
        links.stream()
                .filter(link -> link.lanegroups_flwdn.size()>=2)
                .forEach(link -> perform_lane_changes(link,timestamp));

        // update demand and supply
        // (cell.veh_dwn,cell.veh_out -> cell.demand_dwn , cell.demand_out)
        // (cell.veh_dwn,cell.veh_out -> cell.supply)
        links.forEach(link -> update_supply_demand(link));

        // compute node inflow and outflow (all nodes except sources)
        node_models.forEach(n->n.update_flow(timestamp));

    }

    public void update_macro_flow_part_II(float timestamp) throws OTMException {

        // exchange packets
        for(MacroNodeModel node_model : node_models) {

            // flows on road connections arrive to links on give lanes
            // convert to packets and send
            for(models.ctm.RoadConnection rc : node_model.rcs.values()) {
                Link link = rc.rc.get_end_link();
                link.model.add_vehicle_packet(link,timestamp, new PacketLink(rc.f_rs, rc.rc.out_lanegroups));
            }

            // set exit flows on non-sink lanegroups
            for(UpLaneGroup ulg : node_model.ulgs.values())
                ulg.lg.release_vehicles(ulg.f_is);
        }

        // update cell boundary flows
        links.forEach(link -> update_dwn_flow(link));

    }
}
