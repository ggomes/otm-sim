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
import output.animation.AbstractLinkInfo;
import runner.Scenario;
import utils.OTMUtils;

import java.util.Set;

public abstract class AbstractDiscreteTimeModel extends AbstractModel {

    public float dt;
    public float max_cell_length;
    public Set<Node> nodes;


    //    public Set<models.ctm.LinkModel> macro_link_models = new HashSet<>();
//    public Set<Node> macro_internal_nodes = new HashSet<>();
//    public Set<models.ctm.Source> macro_sources = new HashSet<>();

    public AbstractDiscreteTimeModel(Set<Link> links,String name,boolean is_default,Float dt,Float max_cell_length) {
        super(links,name,is_default);
        this.model_type = ModelType.discrete_time;
        this.dt = dt==null ? -1 : dt;
        this.max_cell_length = max_cell_length==null ? -1 : max_cell_length;

//        // populate macro_internal_nodes: connected in any way to ctm models, minus sources and sinks
//        Set<Node> all_nodes = macro_link_models.stream().map(x->x.link.start_node).collect(toSet());
//        all_nodes.addAll(macro_link_models.stream().map(x->x.link.end_node).collect(toSet()));
//        all_nodes.removeAll(nodes.values().stream().filter(node->node.is_source || node.is_sink).collect(Collectors.toSet()));
//        nodes = all_nodes;
//
//        // give them models.ctm node models
//        for(Node node : macro_internal_nodes)
//            node.set_macro_model( new NodeModel(node) );
    }

    @Override
    public void build(Link link) {
        create_cells(link,max_cell_length);
    }

    @Override
    public void register_first_events(Scenario scenario, Dispatcher dispatcher,float start_time) {
        dispatcher.register_event(new EventMacroFlowUpdate(dispatcher, start_time + scenario.sim_dt, scenario.network));
        dispatcher.register_event(new EventMacroStateUpdate(dispatcher, start_time + scenario.sim_dt, scenario.network));
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

}
