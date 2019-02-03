package models;

import actuator.ActuatorFD;
import commodity.Commodity;
import commodity.Path;
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
import profiles.DemandProfile;
import runner.Scenario;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class AbstractModel {

    public Set<Link> links;
    public String name;
    public boolean is_default;

    public AbstractModel(String name, boolean is_default){
        this.name = name;
        this.is_default = is_default;
    }

    //////////////////////////////////////////////////
    // load
    //////////////////////////////////////////////////

    // called Link.validate
    abstract public void validate(OTMErrorLog errorLog);

    // NOT USED
    abstract public void reset(Link link);

    abstract public void build();

    //////////////////////////////////////////////////
    // factory
    //////////////////////////////////////////////////
    abstract public AbstractOutput create_output_object(Scenario scenario, String prefix, String output_folder, OutputRequest jaxb_or)  throws OTMException;
    abstract public AbstractLaneGroup create_lane_group(Link link, Side side, FlowDirection flowdir, Float length, int num_lanes,int start_lane,Set<RoadConnection> out_rcs);
    abstract public AbstractSource create_source(Link origin, DemandProfile demand_profile, Commodity commodity, Path path);
    abstract public AbstractLinkInfo get_link_info(Link link);
    abstract public AbstractPacketLaneGroup create_lanegroup_packet();

    //////////////////////////////////////////////////
    // run
    //////////////////////////////////////////////////

    // called by OTM.advance
    abstract public void register_with_dispatcher(Scenario scenario, Dispatcher dispatcher, float start_time);

    // called by AbstractModel.add_vehicle_packet
    abstract public Map<AbstractLaneGroup,Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups);

    //////////////////////////////////////////////////
    // partial implementation
    //////////////////////////////////////////////////

    public void set_links(Set<Link> links){
        this.links = links;
    }

    public void initialize(Scenario scenario) throws OTMException {

        for(Link link : links){
            for(AbstractLaneGroup lg : link.lanegroups_flwdn.values())
                lg.allocate_state();
        }
    }

//    public float get_max_vehicles(Link link){
//        return (float) link.lanegroups_flwdn.values().stream().map(x->x.max_vehicles).mapToDouble(i->i).sum();
//    }

    // called by Network constructor
    public void set_road_param(Link link, jaxb.Roadparam r){
        link.road_param = r;
    }

    //////////////////////////////////////////////////
    // final
    //////////////////////////////////////////////////

    // called by ActuatorFD
    final public void set_road_param(Link link, ActuatorFD.FDCommand fd_comm){
        jaxb.Roadparam r = new jaxb.Roadparam();
        r.setJamDensity(fd_comm.jam_density_vpkpl);      //roadparam.jam_density 	... veh/km/lane
        r.setCapacity(fd_comm.capacity_vphpl);        //roadparam.capacity 		... veh/hr/lane
        r.setSpeed(fd_comm.max_speed_kph);           //roadparam.speed 		... km/hr
        set_road_param(link,r);
    }

    // add a vehicle packet that is already known to fit.
    final public void add_vehicle_packet(Link link,float timestamp, PacketLink vp) throws OTMException {

        if(vp.isEmpty())
            return;

        // 1. split arriving packet into subpackets per downstream link.
        // This assigns states to the packets, but
        // This does not set AbstractPacketLaneGroup.target_road_connection
        Map<Long, AbstractPacketLaneGroup> split_packets = link.split_packet(vp);

        // 2. Compute the proportions to apply to the split packets to distribute
        // amongst lane groups
        Map<AbstractLaneGroup,Double> lg_prop = lanegroup_proportions(vp.road_connection.out_lanegroups);

        // 3. distribute the packets
        for(Map.Entry<Long, AbstractPacketLaneGroup> e1 : split_packets.entrySet()){
            Long next_link_id = e1.getKey();
            AbstractPacketLaneGroup packet = e1.getValue();

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

//    private AbstractPacketLaneGroup cast_to_single_lanegroup(PacketLink vp, Long outlink_id){
//
//        // create a lane group packet of appropriate type
//        AbstractPacketLaneGroup split_packet = null; XXXXXXXXXXXXXx
////        try {
////            split_packet = (AbstractPacketLaneGroup) packet_class.newInstance();
////        } catch (InstantiationException e) {
////            e.printStackTrace();
////            return null;
////        } catch (IllegalAccessException e) {
////            e.printStackTrace();
////            return null;
////        }
//
//        boolean has_macro = !vp.no_macro();
//        boolean has_micro = !vp.no_micro();
//
//        // process the fluid state
//        if(has_macro) {
//            for (Map.Entry<KeyCommPathOrLink, Double> e : vp.state2vehicles.entrySet()) {
//                KeyCommPathOrLink key = e.getKey();
//                Double vehicles = e.getValue();
//                if (key.isPath || outlink_id==null)  // null occurs for sinks
//                    split_packet.add_fluid(key,vehicles);
//                else
//                    split_packet.add_fluid(new KeyCommPathOrLink(key.commodity_id, outlink_id, false),vehicles);
//            }
//        }
//
//        // process the vehicle state
//        if(has_micro){
//            for(AbstractVehicle vehicle : vp.vehicles){
//                KeyCommPathOrLink key = vehicle.get_key();
//                // NOTE: We do not update the next link id when it is null. This happens in
//                // sinks. This means that the state in a sink needs to be interpreted
//                // differently, which must be accounted for everywhere.
//                if(key.isPath || outlink_id==null)
//                    split_packet.add_vehicle(key,vehicle);
//                else {
//                    vehicle.set_next_link_id(outlink_id);
//                    split_packet.add_vehicle(new KeyCommPathOrLink(key.commodity_id, outlink_id, false),vehicle);
//                }
//            }
//        }
//
//        return split_packet;
//    }

}
