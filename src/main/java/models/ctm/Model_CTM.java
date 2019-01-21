package models.ctm;

import commodity.Commodity;
import commodity.Path;
import commodity.Subnetwork;
import common.AbstractSource;
import common.Node;
import common.RoadConnection;
import dispatch.Dispatcher;
import dispatch.EventMacroFlowUpdate;
import dispatch.EventMacroStateUpdate;
import error.OTMException;
import geometry.FlowDirection;
import jaxb.OutputRequest;
import models.AbstractLaneGroup;
import common.Link;
import error.OTMErrorLog;
import geometry.Side;
import keys.KeyCommPathOrLink;
import models.AbstractModel;
import models.MacroNodeModel;
import output.AbstractOutput;
import output.animation.AbstractLinkInfo;
import packet.PacketLink;
import profiles.DemandProfile;
import runner.Scenario;
import utils.OTMUtils;

import java.util.*;

import static java.util.stream.Collectors.toSet;

public class Model_CTM extends AbstractModel {

    public float dt;
    public float max_cell_length;
    public Set<MacroNodeModel> node_models;

    public Model_CTM(String name,boolean is_default, Float dt, Float max_cell_length) {
        super(name,is_default);
//        this.model_type = ModelType.discrete_time;
        this.dt = dt==null ? -1 : dt;
        this.max_cell_length = max_cell_length==null ? -1 : max_cell_length;
        myPacketClass = models.ctm.PacketLaneGroup.class;
    }

    //////////////////////////////////////////////////////////////
    // AbstractModel -- abstract methods
    //////////////////////////////////////////////////////////////

    @Override
    public Map<AbstractLaneGroup,Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups) {
        Map<AbstractLaneGroup,Double> A = new HashMap<>();
        double total_supply = candidate_lanegroups.stream().mapToDouble(x->x.get_supply()).sum();
        for(AbstractLaneGroup laneGroup : candidate_lanegroups)
            A.put(laneGroup , laneGroup.get_supply() / total_supply);
        return A;
    }

    @Override
    public AbstractOutput create_output_object(Scenario scenario, String prefix, String output_folder, OutputRequest jaxb_or)  throws OTMException {
        AbstractOutput output = null;
        switch (jaxb_or.getQuantity()) {
            case "cell_veh":
                Long commodity_id = jaxb_or.getCommodity();
                Float outDt = jaxb_or.getDt();
                output = new OutputCellVehicles(scenario, this,prefix, output_folder, commodity_id, outDt);
                break;
            default:
                throw new OTMException("Bad output identifier : " + jaxb_or.getQuantity());
        }
        return output;
    }

    @Override
    public void register_first_events(Scenario scenario, Dispatcher dispatcher, float start_time) {
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

    @Override
    public void validate(Link link,OTMErrorLog errorLog) {
    }

    @Override
    public void reset(Link link) {
        for(AbstractLaneGroup alg : link.lanegroups_flwdn.values()){
            LaneGroup lg = (LaneGroup) alg;
            lg.cells.forEach(x->x.reset());
        }
    }

    @Override
    public void build(Link link) {

        // build node models
        node_models.forEach(m->m.build());

        // create cells
        create_cells(link,max_cell_length);
    }

    @Override
    public AbstractLaneGroup create_lane_group(Link link, Side side, FlowDirection flowdir, Float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs) {
        return new models.ctm.LaneGroup(link,side,flowdir,length,num_lanes,start_lane,out_rcs);
    }

    @Override
    public AbstractLinkInfo get_link_info(Link link) {
        return new output.animation.macro.LinkInfo(link);
    }

    @Override
    public AbstractSource create_source(Link origin, DemandProfile demand_profile, Commodity commodity, Path path) {
        return new models.ctm.Source(origin,demand_profile,commodity,path);
    }

    //////////////////////////////////////////////////////////////
    // AbstractModel -- method completion
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
            node_models.add( new MacroNodeModel(node) );
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);

        for(MacroNodeModel node_model : node_models)
            node_model.initialize(scenario);
    }

    @Override
    public void set_road_param(Link link,jaxb.Roadparam r) {

        super.set_road_param(link,r);

        if(Float.isNaN(dt))
            return;

        // adjustment for MN model
        // TODO REIMPLIMENT MN
//        if(link.model_type==Link.ModelType.mn)
//            r.setJamDensity(Float.POSITIVE_INFINITY);

        // normalize
        float dt_hr = dt/3600f;
        float capacity_vehperlane = r.getCapacity()*dt_hr;

        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values()) {

            float cell_length = lg.length / ((LaneGroup) lg).cells.size() / 1000f;
            float jam_density_vehperlane = r.getJamDensity() * cell_length;
            float ffspeed_veh = r.getSpeed() * dt_hr / cell_length;

            lg.set_road_params(r);
            ((LaneGroup) lg).cells.forEach(c -> c.set_road_params(capacity_vehperlane, jam_density_vehperlane, ffspeed_veh));
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

    public void perform_lane_changes(Link link,float timestamp) {

        // WARNING: THIS ASSUMES NO ADDLANES (lanegroups_flwdn=all lanegroups)

        int cells_in_full_lg = ((LaneGroup)link.lanegroups_flwdn.values().iterator().next()).cells.size();

        // scan cross section from upstream to downstream
        for (int i = 0; i < cells_in_full_lg; i++) {

            Map<Long, Double> gamma = new HashMap<>();

            // compute total flows reduction for each lane group
            for (AbstractLaneGroup lg : link.lanegroups_flwdn.values()) {
                Cell cell = ((LaneGroup) lg).cells.get(i);
                double demand_to_me = 0d;
                if (lg.neighbor_in != null)
                    demand_to_me += ((LaneGroup) lg.neighbor_in).cells.get(i).total_vehs_out;
                if (lg.neighbor_out != null)
                    demand_to_me += ((LaneGroup) lg.neighbor_out).cells.get(i).total_vehs_in;

                // TODO: extract xi as a parameter
                double supply = 0.9d * (1d - cell.wspeed_norm) * (cell.jam_density_veh - cell.get_vehicles());
                gamma.put(lg.id, demand_to_me > supply ? supply / demand_to_me : 1d);
            }

            // lane change flow
            // WARNING: This assumes that no state has vehicles going in both directions.
            // ie a flow that goes left does not also go right. Otherwise I think there may
            // be "data races", where the result depends on the order of lgs.
            for (AbstractLaneGroup alg : link.lanegroups_flwdn.values()) {
                models.ctm.LaneGroup lg = (models.ctm.LaneGroup) alg;
                double my_gamma = gamma.get(lg.id);

                if (lg.neighbor_in != null) {

                    LaneGroup from_lg = (LaneGroup) lg.neighbor_in;
                    Cell from_cell = from_lg.cells.get(i);
                    Map<KeyCommPathOrLink, Double> from_vehs = from_cell.veh_out;

                    ///////////////////////////////////////////////////////////////////////////////////////////

                    for (Map.Entry<KeyCommPathOrLink, Double> e : from_vehs.entrySet()) {
                        Double from_veh = e.getValue();
                        KeyCommPathOrLink state = e.getKey();

                        if (from_veh > OTMUtils.epsilon) {

                            Cell to_cell = lg.cells.get(i);
                            double flw = my_gamma  * from_veh;

                            // remove from this cell
                            from_vehs.put(state, from_veh-flw );
                            from_cell.total_vehs_out -= flw;

                            // add to side cell
                            Side newside = lg.state2lanechangedirection.containsKey(state) ?
                                    lg.state2lanechangedirection.get(state) :
                                    Side.full;
                            switch (newside) {
                                case in:
                                    to_cell.veh_in.put(state, to_cell.veh_in.get(state) + flw);
                                    to_cell.total_vehs_in += flw;
                                    break;
                                case full:
                                    to_cell.veh_dwn.put(state, to_cell.veh_dwn.get(state) + flw);
                                    to_cell.total_vehs_dwn += flw;
                                    break;
                                case out:
                                    to_cell.veh_out.put(state, to_cell.veh_out.get(state) + flw);
                                    to_cell.total_vehs_out += flw;
                                    break;
                            }
                        }
                    }
                    ///////////////////////////////////////////////////////////////////////////////////////////
                }
                if (lg.neighbor_out != null) {

                    LaneGroup from_lg = (LaneGroup) lg.neighbor_out;
                    Cell from_cell = from_lg.cells.get(i);
                    Map<KeyCommPathOrLink, Double> from_vehs = from_cell.veh_in;

                    ///////////////////////////////////////////////////////////////////////////////////////////

                    for (Map.Entry<KeyCommPathOrLink, Double> e : from_vehs.entrySet()) {
                        Double from_veh = e.getValue();
                        KeyCommPathOrLink state = e.getKey();

                        if (from_veh > OTMUtils.epsilon) {

                            Cell to_cell = lg.cells.get(i);
                            double flw = my_gamma * from_veh;

                            // remove from this cell
                            from_vehs.put(state, from_veh-flw );
                            from_cell.total_vehs_in -= flw;

                            // add to side cell
                            Side newside = lg.state2lanechangedirection.containsKey(state) ?
                                    lg.state2lanechangedirection.get(state) :
                                    Side.full;
                            switch (newside) {
                                case in:
                                    to_cell.veh_in.put(state, to_cell.veh_in.get(state) + flw);
                                    to_cell.total_vehs_in += flw;
                                    break;
                                case full:
                                    to_cell.veh_dwn.put(state, to_cell.veh_dwn.get(state) + flw);
                                    to_cell.total_vehs_dwn += flw;
                                    break;
                                case out:
                                    to_cell.veh_out.put(state, to_cell.veh_out.get(state) + flw);
                                    to_cell.total_vehs_out += flw;
                                    break;
                            }
                        }
                    }


                    ///////////////////////////////////////////////////////////////////////////////////////////
                }
            }
        }

    }

    // call update_supply_demand on each cell
    public void update_supply_demand(Link link) {
        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values()) {
            LaneGroup ctmlg = (LaneGroup) lg;
            if(!ctmlg.states.isEmpty())
                ctmlg.cells.forEach(cell -> cell.update_supply_demand());
        }
    }

    public void update_dwn_flow(Link link) {
        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values())
            ((LaneGroup) lg).update_dwn_flow();
    }

    public void update_state(float timestamp,Link link) {
        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values())
            ((LaneGroup) lg).update_state(timestamp);
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


    ///////////////////////////////////////////
    // private
    ///////////////////////////////////////////

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
