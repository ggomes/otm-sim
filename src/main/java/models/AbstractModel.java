package models;

import common.AbstractLaneGroup;
import common.Link;
import error.OTMException;
import packet.PacketLaneGroup;
import packet.PacketLink;
import common.Scenario;
import utils.StochasticProcess;

import java.util.Map;
import java.util.Set;

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
    public final boolean is_default;
    public final StochasticProcess stochastic_process;
    public Set<Link> links;

    //////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////

    public AbstractModel(Type type,String name, boolean is_default, StochasticProcess process){
        this.type = type;
        this.name = name;
        this.is_default = is_default;
        this.stochastic_process = process;
    }

    public abstract void build() throws OTMException;

    public void set_links(Set<Link> links){
        this.links = links;
    }

    //////////////////////////////////////////////////
    // extendable
    //////////////////////////////////////////////////

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

}
