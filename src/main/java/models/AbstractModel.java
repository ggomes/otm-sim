package models;

import actuator.ActuatorFD;
import commodity.Commodity;
import commodity.Path;
import commodity.Subnetwork;
import common.AbstractSource;
import common.Link;
import common.RoadConnection;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import geometry.FlowDirection;
import geometry.Side;
import jaxb.OutputRequest;
import output.AbstractOutput;
import output.animation.AbstractLinkInfo;
import packet.AbstractPacketLaneGroup;
import packet.PacketLink;
import packet.PacketSplitter;
import profiles.DemandProfile;
import runner.Scenario;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class AbstractModel {

//    public enum ModelType {discrete_time,discrete_event};

    public Class myPacketClass;
//    public ModelType model_type;

    public Set<Link> links;
    public String name;
    public boolean is_default;

    public AbstractModel(String name, boolean is_default){
        this.name = name;
        this.is_default = is_default;
    }

    //////////////////////////////////////////////////////////////
    // abstract methods
    //////////////////////////////////////////////////////////////

    abstract public Map<AbstractLaneGroup,Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups);
    abstract public AbstractOutput create_output_object(Scenario scenario, String prefix, String output_folder, OutputRequest jaxb_or)  throws OTMException;
    abstract public void register_first_events(Scenario scenario, Dispatcher dispatcher, float start_time);

    // LINK LEVEL INTERFACE
    abstract public void register_commodity(Link link, Commodity comm, Subnetwork subnet) throws OTMException;
    abstract public void validate(Link link, OTMErrorLog errorLog);
    abstract public void reset(Link link);
    abstract public void build(Link link);

    abstract public AbstractLaneGroup create_lane_group(Link link, Side side, FlowDirection flowdir, Float length, int num_lanes,int start_lane,Set<RoadConnection> out_rcs);
    abstract public AbstractLinkInfo get_link_info(Link link);
    abstract public AbstractSource create_source(Link origin, DemandProfile demand_profile, Commodity commodity, Path path);

    //////////////////////////////////////////////////

    public void set_links(Set<Link> links){
        this.links = links;
    }

    public void initialize(Scenario scenario) throws OTMException {
        for(Link link : links){
            // allocate state for each lanegroup in this link
            for(AbstractLaneGroup lg : link.lanegroups_flwdn.values() )
                lg.allocate_state();
        }
    }

//    public float get_max_vehicles(Link link){
//        return (float) link.lanegroups_flwdn.values().stream().map(x->x.max_vehicles).mapToDouble(i->i).sum();
//    }

    public void set_road_param(Link link, jaxb.Roadparam r){
        link.road_param = r;
    }

    //////////////////////////////////////////////////
    // Final
    //////////////////////////////////////////////////

    final public void build(){
        for(Link link : links)
            build(link);
    }

    final public void set_road_param(Link link, ActuatorFD.FDCommand fd_comm){
        jaxb.Roadparam r = new jaxb.Roadparam();
        r.setJamDensity(fd_comm.jam_density_vpkpl);      //roadparam.jam_density 	... veh/km/lane
        r.setCapacity(fd_comm.capacity_vphpl);        //roadparam.capacity 		... veh/hr/lane
        r.setSpeed(fd_comm.max_speed_kph);           //roadparam.speed 		... km/hr
        set_road_param(link,r);
    }

    public void add_vehicle_packet(Link link,float timestamp, PacketLink vp) throws OTMException {

        if(vp.isEmpty())
            return;

        // sink or many-to-one
        // this implies that next-link is trivial
        // and (for now) target_lanegroup is trivial
        if(link.packet_splitter==null){
            // if sink, encode by using current link id as nextlink.
            Long outlink_id = link.is_sink ? link.getId() : link.end_node.out_links.values().iterator().next().getId();
            AbstractPacketLaneGroup packet = PacketSplitter.cast_packet_null_splitter(myPacketClass,vp,outlink_id);
            AbstractLaneGroup join_lanegroup = vp.arrive_to_lanegroups.iterator().next();
            join_lanegroup.add_native_vehicle_packet(timestamp,packet);
            return;
        }

        // tag the packet with next_link and target_lanegroups
        Map<Long, AbstractPacketLaneGroup> split_packets = link.packet_splitter.split_packet(myPacketClass,vp);

        // process each split packet
        for(Map.Entry<Long, AbstractPacketLaneGroup> e : split_packets.entrySet()){

            Long outlink_id = e.getKey();
            AbstractPacketLaneGroup split_packet = e.getValue();

            if(split_packet.isEmpty())
                continue;

            // TODO: THIS IS NO LONGER NEEDED
            split_packet.target_lanegroups = link.outlink2lanegroups.get(outlink_id);

            if(split_packet.target_lanegroups==null) {

                // In MPI, the link may not be present, so don't throw and exception.
                continue;

//                throw new OTMException(String.format("target_lanegroups==null.\nThis may be an error in split ratios. " +
//                        "There is no access from link " + link.getId() + " to " +
//                        "link " + outlink_id + ". A possible cause is that there is " +
//                        "a positive split ratio between these two links."));
            }

            Set<AbstractLaneGroup> candidate_lanegroups = vp.arrive_to_lanegroups;

            // split the split_packet amongst the candidate lane groups.
            // then add them
            if(candidate_lanegroups.size()==1) {
                AbstractLaneGroup laneGroup = candidate_lanegroups.iterator().next();
                laneGroup.add_native_vehicle_packet(timestamp, split_packet);
            } else {
                for (Map.Entry<AbstractLaneGroup, Double> ee : lanegroup_proportions(candidate_lanegroups).entrySet()) {
                    AbstractLaneGroup laneGroup = ee.getKey();
                    Double prop = ee.getValue();
                    if (prop <= 0d)
                        continue;
                    if (prop==1d)
                        laneGroup.add_native_vehicle_packet(timestamp, split_packet );
                    else
                        laneGroup.add_native_vehicle_packet(timestamp, split_packet.times(prop));
                }
            }

//            // TODO: FOR MESO and MICRO MODELS, CHECK THAT THERE IS AT LEAST 1 VEHICLE WORTH OF SUPPLY.
//
//            // if all candidates are full, then choose one that is closest and not full
//            if(join_lanegroup==null) {
//
//                join_lanegroup = choose_closest_that_is_not_full(vp.arrive_to_lanegroups,candidate_lanegroups,split_packet.target_lanegroups);
//
//                // put lane change requests on the target lane groups
//                // TODO: REDO THIS
////                add_lane_change_request(timestamp,lanegroup_packet,join_lanegroup,lanegroup_packet.target_lanegroups,Queue.Type.transit);
//            }

            // add the packet to it

        }

    }
}
