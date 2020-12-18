package models;

import core.*;
import core.geometry.AddLanes;
import core.geometry.Gate;
import core.geometry.Side;
import error.OTMException;
import core.packet.PacketLaneGroup;
import core.packet.PacketLink;
import jaxb.Lanechanges;
import lanechange.LinkLaneSelector;
import utils.OTMUtils;
import utils.StochasticProcess;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * This is the base class for all models in OTM. It is not directly extended
 * by concrete models. Instead these should use one of its children classes:
 * AbstractFluidModel or AbstractVehicleModel. The user need not implement anything
 * from this class directly. All of the abstract methods of AbstractModel have
 * partial implementations in the child classes.
 */
public abstract class AbstractModel implements InterfaceModel {

    public enum Type { None, Fluid, Vehicle }

    public final Type type;
    public final String name;
    public final StochasticProcess stochastic_process;
    public Set<Link> links;
//    public LinkLaneSelector lane_selector;

    //////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////

    public AbstractModel(Type type, String name, Set<Link> links, Collection<RoadConnection> road_connections, StochasticProcess process, Lanechanges lcs) throws OTMException {
        this.type = type;
        this.name = name;
        this.stochastic_process = process;

        // set links
        this.links = links;

        if(links==null || links.isEmpty())
            return;

        Scenario scenario = links.iterator().next().network.scenario;

        // set link models (links will choose new over default, so this determines the link list for each model)
        for (Link link : links) {
            // determine whether link is a relative source link
            // (a relative source is one that has at least one incoming link that is not in the model)
            boolean incoming_are_all_in_model = links.containsAll(link.start_node.in_links.values());
            link.model = this;
            link.is_model_source_link = !link.is_source && !incoming_are_all_in_model;

            // create lane groups
            Set<RoadConnection> out_rc = link.end_node.road_connections.stream()
                    .filter(rc->rc.start_link==link)
                    .collect(toSet());
            create_lane_groups(link, out_rc);

            // populate link.outlink2lanegroups
            if(!link.is_sink) {
                link.outlink2lanegroups = new HashMap<>();
                for(Link outlink : link.end_node.out_links) {
                    Set<AbstractLaneGroup> lgs = link.lgs.stream()
                            .filter(lg -> lg.outlink2roadconnection.containsKey(outlink.getId()))
                            .collect(Collectors.toSet());
                    if(!lgs.isEmpty())
                        link.outlink2lanegroups.put(outlink.getId(), lgs);
                }
            }

        }

        // set out lanegroups on road connections
        for(RoadConnection rc : road_connections) {
            if (rc.end_link != null) {
                rc.out_lanegroups = new HashSet<>();
                // TODO THIS SEEMS SLOW
                for (int lane = rc.end_link_from_lane; lane <= rc.end_link_to_lane; lane++)
                    rc.out_lanegroups.add(rc.end_link.get_lanegroup_for_up_lane(lane));
            }
        }

        // set lane change model .............................................
        String default_type = "keep";
        float default_dt = 0f;

        if(lcs==null) {
            for(Link link : links)
                link.lane_selector = new LinkLaneSelector(default_type,default_dt,null,link,scenario.commodities.keySet());

        } else {
            for(jaxb.Lanechange lc : lcs.getLanechange()){
                Collection<Long> commids = lc.getComms()==null ?
                        scenario.commodities.keySet() :
                        OTMUtils.csv2longlist(lc.getComms());
                for(Link link : links)
                    link.lane_selector = new LinkLaneSelector(lc.getType(),lc.getDt(),lc.getParameters(),link,commids);
            }
        }

    }

    public void initialize(Scenario scenario, float start_time) throws OTMException {
        for(Link link : links){
            for(AbstractLaneGroup lg : link.lgs)
                lg.allocate_state();
        }

        register_with_dispatcher(scenario, scenario.dispatcher, start_time);
    }

    //////////////////////////////////////////////////
    // final
    //////////////////////////////////////////////////

    // add a vehicle core.packet that is already known to fit.
    final public void add_vehicle_packet(Link link,float timestamp, PacketLink vp) throws OTMException {

        if(vp.isEmpty())
            return;

        // 1. split arriving core.packet into subpackets per downstream link.
        // This assigns states to the packets, but
        // This does not set AbstractPacketLaneGroup.target_road_connection
        Map<Long, PacketLaneGroup> split_packets = link.split_packet(vp);

        // 2. Compute the proportions to apply to the split packets to distribute
        // amongst lane groups

        // TODO THIS NEEDS TO ACCOUNT FOR LANE GROUP PROHIBITIONS
        Map<AbstractLaneGroup,Double> lg_prop = lanegroup_proportions(vp.road_connection.out_lanegroups);

        // 3. distribute the packets
        for(Map.Entry<Long, PacketLaneGroup> e1 : split_packets.entrySet()){
            Long next_link_id = e1.getKey();
            PacketLaneGroup packet = e1.getValue();

            if(packet.isEmpty())
                continue;

            for(Map.Entry<AbstractLaneGroup,Double> e2 : lg_prop.entrySet()){
                AbstractLaneGroup join_lg = e2.getKey();
                Double prop = e2.getValue();
                if (prop <= 0d)
                    continue;
                if (prop==1d)
                    join_lg.add_vehicle_packet(timestamp, packet, next_link_id );
                else
                    join_lg.add_vehicle_packet(timestamp, packet.times(prop), next_link_id);
            }
        }

    }

    final public PacketLaneGroup create_lanegroup_packet(){
        return new PacketLaneGroup();
    }

    //////////////////////////////////////////////////
    // private
    //////////////////////////////////////////////////

    private static void create_lane_groups(Link link,final Set<RoadConnection> out_rcs) throws OTMException {

        if (link.model == null)
            throw new OTMException("Not all links have a model.");

        if (out_rcs.isEmpty() && !link.is_sink)
            throw new OTMException("out_rcs.isEmpty() && !link.is_sink FOR LINK "+link.getId());

        // create lanegroups
        link.set_flwdn_lanegroups(create_dnflw_lanegroups(link, out_rcs));

        // set start_lane_up ...................
        int offset = 0;
        if(link.road_geom!=null){
            if(link.road_geom.in_is_full_length())
                offset = 0;
            else {
                int dn_in_lanes = link.road_geom.in != null ? link.road_geom.in.lanes : 0;
                offset = dn_in_lanes;
            }
        }

        for (AbstractLaneGroup lg : link.lgs) {
            switch (lg.side) {
                case in:
                    if(link.road_geom.in_is_full_length())
                        lg.start_lane_up = lg.start_lane_dn;
                    break;
                case middle:
                    lg.start_lane_up = lg.start_lane_dn - offset;
                    break;
                case out:
                    if(link.road_geom.out_is_full_length())
                        lg.start_lane_up = lg.start_lane_dn - offset;
                    break;
            }
        }

        // set neighbors ...................

        // ................... long lanegroups = {dn addlane, stay lgs}
        int num_dn_lanes = link.get_num_dn_lanes();
        if(num_dn_lanes>1) {
            List<AbstractLaneGroup> long_lgs = IntStream.rangeClosed(1, link.get_num_dn_lanes())
                    .mapToObj(lane -> link.dnlane2lanegroup.get(lane)).collect(toList());
            AbstractLaneGroup prev_lg = null;
            for (int lane = 1; lane <= num_dn_lanes; lane++) {

                AbstractLaneGroup lg = long_lgs.get(lane - 1);

                assert(lg!=null);

                if (prev_lg == null)
                    prev_lg = lg;
                if (lg != prev_lg) {
                    lg.neighbor_in = prev_lg;
                    prev_lg.neighbor_out = lg;
                    prev_lg = lg;
                }
            }

            prev_lg = null;
            for(int lane=num_dn_lanes;lane>=1;lane--){
                AbstractLaneGroup lg = long_lgs.get(lane-1);
                if(prev_lg==null)
                    prev_lg = lg;
                if(lg!=prev_lg) {
                    lg.neighbor_out = prev_lg;
                    prev_lg.neighbor_in = lg;
                    prev_lg = lg;
                }
            }
        }

        // set barriers .................
        if(link.road_geom!=null){
            if(link.road_geom.in !=null && !link.road_geom.in.isopen)
                link.in_barriers = generate_barriers(link,link.road_geom.in);
            if(link.road_geom.out !=null && !link.road_geom.out.isopen)
                link.out_barriers = generate_barriers(link,link.road_geom.out);
        }

    }

    private static List<AbstractLaneGroup> create_dnflw_lanegroups(Link link, Set<RoadConnection> out_rcs) throws OTMException {
        // Find unique subsets of road connections, and create a lane group for each one.

        List<AbstractLaneGroup> lanegroups = new ArrayList<>();

        // empty out_rc <=> sink
        assert(out_rcs.isEmpty()==link.is_sink);

        int start_lane = 1;

        // inner addlane ..................................
        if(link.road_geom!=null && link.road_geom.in !=null){

            // collect road connections for this addlane
            final int end_lane = link.road_geom.in.lanes;
            Set<RoadConnection> myrcs = out_rcs.stream()
                    .filter(rc->rc.start_link_from_lane <= end_lane)
                    .collect(toSet());

            // add lanes have either no road connection or all
            // road connections span all lanes.
            if(myrcs!=null && !myrcs.isEmpty() && !myrcs.stream().allMatch(rc-> rc.start_link_from_lane==1 && rc.start_link_to_lane>=end_lane))
                throw new OTMException("Road connections do not conform to rules.");

            // create the lane group
            lanegroups.add(create_dnflw_lanegroup(link,
                    1,
                    link.road_geom.in.lanes,
                    myrcs));

            start_lane = end_lane + 1;
        }

        // middle lanes .................................
        final int fstartlane = start_lane;
        Set<RoadConnection> prevrcs = out_rcs.stream()
                .filter(rc->rc.start_link_from_lane <= fstartlane &&
                        rc.start_link_to_lane >= fstartlane)
                .collect(toSet());

        int lg_start_lane = start_lane;
        int lane;
        for(lane=start_lane+1;lane<start_lane+link.full_lanes;lane++) {
            final int flane = lane;
            Set<RoadConnection> myrcs = out_rcs.stream()
                    .filter(rc->rc.start_link_from_lane <= flane &&
                            rc.start_link_to_lane >= flane)
                    .collect(toSet());
            if(!myrcs.equals(prevrcs)){
                lanegroups.add(create_dnflw_lanegroup(link,
                        lg_start_lane,
                        lane-lg_start_lane,
                        prevrcs));
                prevrcs = myrcs;
                lg_start_lane = lane;
            }
        }

        lanegroups.add(create_dnflw_lanegroup(link,
                lg_start_lane,
                lane-lg_start_lane,
                prevrcs));

        // outer addlane ..................................
        if(link.road_geom!=null && link.road_geom.out !=null){

            final int fstart_lane = start_lane + link.full_lanes;
            final int fend_lane = fstart_lane + link.road_geom.out.lanes -1;

            // collect road connections for this addlane
            final int end_lane = start_lane+link.road_geom.out.lanes-1;
            Set<RoadConnection> myrcs = out_rcs==null ? null : out_rcs.stream()
                    .filter(rc->rc.start_link_to_lane >= fstart_lane )
                    .collect(toSet());

            // add lanes have either no road connection or all
            // road connections span all lanes.
            if(myrcs!=null && !myrcs.isEmpty() && !myrcs.stream().allMatch(rc-> rc.start_link_from_lane<=fstart_lane && rc.start_link_to_lane==fend_lane))
                throw new OTMException("Road connections do not conform to rules.");

            // create the lane group
            lanegroups.add(create_dnflw_lanegroup(link,
                    fstart_lane,
                    link.road_geom.out.lanes,
                    myrcs));

        }

        return lanegroups;
    }

    private static AbstractLaneGroup create_dnflw_lanegroup(Link link, int dn_start_lane, int num_lanes, Set<RoadConnection> out_rcs) throws OTMException {

        // Determine whether it is an addlane lanegroup or a full lane lane group.
        Set<Side> sides = new HashSet<>();
        for(int lane=dn_start_lane;lane<dn_start_lane+num_lanes;lane++)
            sides.add(link.get_side_for_dn_lane(lane));

        // all lanes must belong to one of the 3
        // That is, there are no lane groups that is both inner and full length, or outer and full length.
//        if(sides.size()!=1)
//            throw new OTMException(String.format("Rule broken: Lane groups must be contained in addlanes or stay lanes. Check link %d",link.getId()));

        jaxb.Roadparam rp = null;
        float length = 0f;
        Side side = sides.iterator().next();
        switch(side){
            case in:    // inner addlane lane group
                rp = link.road_geom.in.roadparam;
                length = link.road_geom.in.get_length(link.length);
                break;
            case middle:    // full lane lane group
                rp = link.road_param_full;
                length = link.length;
                break;
            case out:    // outer addlane lane group
                rp = link.road_geom.out.roadparam;
                length = link.road_geom.out.get_length(link.length);
                break;
        }

        // This precludes multiple lane groups of the same side: multiple 'stay' lane
        return link.model.create_lane_group(link,side,length,num_lanes,dn_start_lane,out_rcs,rp);
    }

    private static HashSet<Barrier> generate_barriers(Link link, AddLanes addlanes){
        HashSet<Barrier> X = new HashSet<>();
        List<Float> gate_points = new ArrayList<>();
        gate_points.add(0f);
        gate_points.add(link.length);
        for(Gate gate : addlanes.gates){
            gate_points.add(gate.start_pos);
            gate_points.add(gate.end_pos);
        }
        Collections.sort(gate_points);
        for(int i=0;i<gate_points.size();i+=2){
            float bstart = gate_points.get(i);
            float bend = gate_points.get(i+1);
            X.add(new Barrier(bstart,bend));
        }
        return X;
    }

    private static void assign_lane_change_models(Set<Long> allcommids,Map<Long,Link> links,jaxb.Lanechanges jlcs) throws OTMException {

//        String default_type = "keep";
//        float default_dt = 0f;
//
//        if(jlcs==null) {
//            for(Link link : links.values())
//                for(AbstractLaneGroup lg : link.lgs)
//                    lg.assign_lane_selector(default_type,default_dt,null,allcommids);
//            return;
//        }
//
//        Set<Long> unassigned = new HashSet<>(links.keySet());
//        for(jaxb.Lanechange lc : jlcs.getLanechange()){
//            String type = lc.getType();
//            Collection<Long> linkids = lc.getLinks()==null ? links.keySet() : OTMUtils.csv2longlist(lc.getLinks());
//            Collection<Long> commids = lc.getComms()==null ? allcommids : OTMUtils.csv2longlist(lc.getComms());
//            unassigned.removeAll(linkids);
//            for(Long linkid : linkids)
//                if(links.containsKey(linkid))
//                    for(AbstractLaneGroup lg : links.get(linkid).lgs)
//                        lg.assign_lane_selector(type,lc.getDt(),lc.getParameters(),commids);
//        }
//
//        if(!unassigned.isEmpty()){
//            Optional<jaxb.Lanechange> x = jlcs.getLanechange().stream().filter(xx -> xx.isIsDefault()).findFirst();
//            String my_default_type = x.isPresent() ? x.get().getType() : default_type;
//            float my_dt = x.isPresent() ? x.get().getDt() : default_dt;
//            jaxb.Parameters my_params = x.isPresent() ? x.get().getParameters() : null;
//            for(Long linkid : unassigned)
//                for(AbstractLaneGroup lg : links.get(linkid).lgs)
//                    lg.assign_lane_selector(my_default_type,my_dt,my_params,allcommids);
//        }

    }

}
