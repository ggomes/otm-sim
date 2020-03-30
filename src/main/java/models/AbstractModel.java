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
import geometry.FlowPosition;
import geometry.Side;
import jaxb.OutputRequest;
import output.AbstractOutput;
import output.animation.AbstractLinkInfo;
import packet.PacketLaneGroup;
import packet.PacketLink;
import profiles.DemandProfile;
import runner.Scenario;
import utils.OTMUtils;
import utils.StochasticProcess;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class AbstractModel {

    public enum Type { None, Fluid, Vehicle }

    public final Type type;
    public final String name;
    public final boolean is_default;
    public final StochasticProcess stochastic_process;
    public Set<Link> links;

    public AbstractModel(Type type,String name, boolean is_default, StochasticProcess process){
        this.type = type;
        this.name = name;
        this.is_default = is_default;
        this.stochastic_process = process;
    }

    //////////////////////////////////////////////////
    // abstract methods
    //////////////////////////////////////////////////

    public abstract void validate(OTMErrorLog errorLog);
    public abstract void reset(Link link);
    public abstract void build();
    public abstract AbstractOutput create_output_object(Scenario scenario, String prefix, String output_folder, OutputRequest jaxb_or)  throws OTMException;
    public abstract AbstractLaneGroup create_lane_group(Link link, Side side, FlowPosition flwpos, Float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs);
    public abstract AbstractSource create_source(Link origin, DemandProfile demand_profile, Commodity commodity, Path path);
    public abstract AbstractLinkInfo get_link_info(Link link);
    public abstract void register_with_dispatcher(Scenario scenario, Dispatcher dispatcher, float start_time);
    public abstract Map<AbstractLaneGroup,Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups);

    //////////////////////////////////////////////////
    // partially implemented methods
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

    // called by Network constructor
    public void set_road_param(Link link, jaxb.Roadparam r){
        link.road_param = r;
        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values())
            lg.set_road_params(r);
    }

    //////////////////////////////////////////////////
    // fully implemented methods
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
        Map<Long, PacketLaneGroup> split_packets = link.split_packet(vp);

        // 2. Compute the proportions to apply to the split packets to distribute
        // amongst lane groups
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

    final public Float get_waiting_time_sec(double rate_vps){
        return OTMUtils.get_waiting_time(rate_vps,stochastic_process);
    }

}
