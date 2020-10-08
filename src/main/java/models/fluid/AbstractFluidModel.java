package models.fluid;

import commodity.Commodity;
import commodity.Path;
import common.*;
import dispatch.Dispatcher;
import error.OTMException;
import geometry.FlowPosition;
import geometry.Side;
import keys.State;
import models.AbstractModel;
import models.fluid.nodemodel.NodeModel;
import models.fluid.nodemodel.RoadConnection;
import models.fluid.nodemodel.UpLaneGroup;
import output.animation.AbstractLinkInfo;
import packet.PacketLink;
import profiles.DemandProfile;
import utils.OTMUtils;
import utils.StochasticProcess;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public abstract class AbstractFluidModel extends AbstractModel implements InterfaceFluidModel {

    public final float max_cell_length;
    public final float dt_sec;
    protected Set<Link> source_links;
    protected Set<Link> sink_links;
    protected Map<Long, NodeModel> node_models;

    public AbstractFluidModel(String name, boolean is_default, float dt_sec, StochasticProcess process, Float max_cell_length) {
        super(AbstractModel.Type.Fluid,name, is_default,process);
        this.dt_sec = dt_sec;
        this.max_cell_length = max_cell_length==null ? -1 : max_cell_length;
    }

    //////////////////////////////////////////////////////////////
    // final for fluid models
    //////////////////////////////////////////////////////////////

    public final void build() throws OTMException {

        if(Float.isNaN(dt_sec))
            return;

        // normalize
        float dt_hr = dt_sec /3600f;

        for(Link link : links) {

            // compute cell length .............
            float r = link.length/max_cell_length;
            boolean is_source_or_sink = link.is_source || link.is_sink;

            int num_cells = is_source_or_sink ?
                    1 :
                    OTMUtils.approximately_equals(r%1.0,0.0) ? (int) r :  1+((int) r);

            float cell_length_meters = link.length/num_cells;

            // create cells ....................
            for (AbstractLaneGroup lg : link.lanegroups_flwdn.values()) {

                FluidLaneGroup flg = (FluidLaneGroup) lg;
                flg.create_cells(this, cell_length_meters);

                // translate parameters to per-cell units
                float cell_length = flg.length / flg.cells.size() / 1000f;
                if (!link.is_source) {
                    flg.nom_ffspeed_cell_per_dt /= cell_length;
                    flg.ffspeed_cell_per_dt /= cell_length;
                    flg.jam_density_veh_per_cell *= cell_length;
                    flg.critical_density_vehpercell *= cell_length;
                    flg.wspeed_cell_per_dt /= cell_length;
                    flg.compute_lcw();
                }

            }

            // barriers .........................
            barriers_to_cells(link,link.in_barriers,cell_length_meters,link.get_num_dn_in_lanes());
            barriers_to_cells(link,link.out_barriers,cell_length_meters,link.get_num_dn_in_lanes()+link.get_num_full_lanes());

        }
        for(NodeModel nm : node_models.values())
            nm.build();
    }

    @Override
    public final void set_links(Set<Link> links) {
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
        }
    }

    //////////////////////////////////////////////////////////////
    // InterfaceModel
    //////////////////////////////////////////////////////////////

    @Override
    public void reset(Link link) {
        for(AbstractLaneGroup alg : link.lanegroups_flwdn.values()){
            FluidLaneGroup lg = (FluidLaneGroup) alg;
            lg.cells.forEach(x->x.reset());
        }
    }

    @Override
    public void register_with_dispatcher(Scenario scenario, Dispatcher dispatcher, float start_time){
        dispatcher.register_event(new EventFluidModelUpdate(dispatcher, start_time + dt_sec, this));
        dispatcher.register_event(new EventFluidStateUpdate(dispatcher, start_time + dt_sec, this));
    }

    @Override
    public AbstractLaneGroup create_lane_group(Link link, Side side, FlowPosition flwpos, Float length, int num_lanes, int start_lane, Set<common.RoadConnection> out_rcs,jaxb.Roadparam rp) {
        return new FluidLaneGroup(link,side,flwpos,length,num_lanes,start_lane,out_rcs,rp);
    }

    @Override
    public final AbstractSource create_source(Link origin, DemandProfile demand_profile, Commodity commodity, Path path) {
        return new FluidSource(origin,demand_profile,commodity,path);
    }

    @Override
    public AbstractLinkInfo get_link_info(Link link) {
        return new output.animation.macro.LinkInfo(link);
    }

    //////////////////////////////////////////////////////////////
    // extendable
    //////////////////////////////////////////////////////////////

    // MOVE THIS TO MODEL CTM

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);

        for(NodeModel node_model : node_models.values())
            node_model.initialize(scenario);
    }

    //////////////////////////////////////////////////////////////
    // state equation
    //////////////////////////////////////////////////////////////

    // called by EventFluidModelUpdate
    public void update_flow(float timestamp) throws OTMException {

        update_flow_I(timestamp);

        // -- MPI communication (in otm-mpi) -- //

        update_flow_II(timestamp);

    }

    // update supplies and demands, then run node model to obtain inter-link flows
    public void update_flow_I(float timestamp) throws OTMException {

        // lane changes and compute demand and supply
        for(Link link : links)
            compute_lanechange_demand_supply(link,timestamp);

        // compute node inflow and outflow (all nodes except sources)
        node_models.values().forEach(n->n.update_flow(timestamp));

    }

    // compute source and source flows
    // node model exchange packets
    public void update_flow_II(float timestamp) throws OTMException {

        // add to source links
        for(Link link : source_links){
            for(AbstractSource asource : link.sources){
                FluidSource source = (FluidSource) asource;
                for(Map.Entry<Long,Map<State,Double>> e : source.source_flows.entrySet()){
                    FluidLaneGroup lg = (FluidLaneGroup) link.lanegroups_flwdn.get(e.getKey());
                    AbstractCell upcell = lg.cells.get(0);
                    upcell.add_vehicles(e.getValue(),null,null);
                }
            }
        }

        // release from sink links
        for(Link link : sink_links){

            for(AbstractLaneGroup alg : link.lanegroups_flwdn.values()) {
                FluidLaneGroup lg = (FluidLaneGroup) alg;
                Map<State,Double> flow_dwn = lg.get_demand();

                lg.release_vehicles(flow_dwn);

                for(Map.Entry<State,Double> e : flow_dwn.entrySet())
                    if(e.getValue()>0)
                        lg.update_flow_accummulators(e.getKey(),e.getValue());
            }

        }

        // node models exchange packets
        for(NodeModel node_model : node_models.values()) {

            // flows on road connections arrive to links on give lanes convert to packets and send
            for(RoadConnection rc : node_model.rcs.values()) {
                Link link = rc.rc.get_end_link();
                link.model.add_vehicle_packet(link,timestamp, new PacketLink(rc.f_rs, rc.rc));
            }

            // set exit flows on non-sink lanegroups
            for(UpLaneGroup ulg : node_model.ulgs.values()) {
                ulg.lg.release_vehicles(ulg.f_gs);

                // send lanegroup exit flow to flow accumulator
                for(Map.Entry<State,Double> e : ulg.f_gs.entrySet())
                    if(e.getValue()>0)
                        ulg.lg.update_flow_accummulators(e.getKey(),e.getValue());
            }

        }

    }

    // called by EventFluidStateUpdate
    // intra link flows and states
    protected void update_fluid_state(float timestamp) throws OTMException {
        for(Link link : links)
            update_link_state(link,timestamp);
    }

    //////////////////////////////////////////////////////////////
    // getters
    //////////////////////////////////////////////////////////////

    public NodeModel get_node_model_for_node(Long node_id){
        return node_models.get(node_id);
    }

    // PRIVATE

    private static void barriers_to_cells(Link link,Set<Barrier> barriers,float cell_length_meters,int in_lane){

        if(barriers==null || barriers.isEmpty())
            return;

        // inner lane group
        FluidLaneGroup inlg = (FluidLaneGroup) link.lanegroups_flwdn.values().stream()
                .filter(lg->lg.start_lane_dn+lg.num_lanes-1==in_lane)
                .findFirst().get();

        // outer full lane
        FluidLaneGroup outlg = (FluidLaneGroup) link.lanegroups_flwdn.values().stream()
                .filter(lg->lg.start_lane_dn==in_lane+1)
                .findFirst().get();

        // loop through inner barriers
        for(Barrier b : barriers){

            int start = Math.round((link.length-b.start)/cell_length_meters);
            int end = Math.round((link.length-b.end)/cell_length_meters);

            if(start>end){

                if(inlg!=null && inlg.cells.size()>=start)
                    for(int i=inlg.cells.size()-start;i<inlg.cells.size()-end;i++)
                        inlg.cells.get(i).out_barrier=true;

                if(outlg!=null && outlg.cells.size()>=start)
                    for(int i=outlg.cells.size()-start;i<outlg.cells.size()-end;i++)
                        outlg.cells.get(i).in_barrier=true;
            }
        }
    }

}
