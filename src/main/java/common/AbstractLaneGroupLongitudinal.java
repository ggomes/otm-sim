package common;

import commodity.Path;
import error.OTMErrorLog;
import error.OTMException;
import geometry.Side;
import keys.KeyCommPathOrLink;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractLaneGroupLongitudinal extends AbstractLaneGroup {

    // map from outlink to road-connection. For one-to-one links with no road connection defined,
    // this returns a null.
    protected Map<Long,RoadConnection> outlink2roadconnection;

    // exiting road connection to the states that use it (should be avoided in the one-to-one case)
    public Map<Long, Set<KeyCommPathOrLink>> roadconnection2states;

    // state to the road connection it must use (should be avoided in the one-to-one case)
    public Map<KeyCommPathOrLink,Long> state2roadconnection;

    abstract public void exiting_roadconnection_capacity_has_been_modified(float timestamp);

    /**
     * An event signals an opportunity to release a vehicle packet. The lanegroup must,
     * 1. construct packets to be released to each of the lanegroups reached by each of it's
     *    road connections.
     * 2. check what portion of each of these packets will be accepted. Reduce the packets
     *    if necessary.
     * 3. call next_link.add_native_vehicle_packet for each reduces packet.
     * 4. remove the vehicle packets from this lanegroup.
     */
    abstract public void release_vehicle_packets(float timestamp) throws OTMException;

    public AbstractLaneGroupLongitudinal(Link link, Side side, float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs) {
        super(link,side,length,num_lanes);
        this.start_lane_dn = start_lane;
        this.outlink2roadconnection = new HashMap<>();
        this.state2roadconnection = new HashMap<>();
        if(out_rcs!=null)
            for(RoadConnection rc : out_rcs)
                outlink2roadconnection.put(rc.end_link.id,rc);
    }

    @Override
    public void delete() {
        super.delete();
        outlink2roadconnection = null;
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);

        // out_road_connections all lead to links that are immediately downstream
        Set dwn_links = link.end_node.out_links.values().stream().map(x->x.id).collect(Collectors.toSet());
        if(!dwn_links.containsAll(outlink2roadconnection.keySet()))
            errorLog.addError("some outlinks are not immediately downstream");

    }

    public void allocate_state(){

        // initialize roadconnection2states
        roadconnection2states = new HashMap<>();
        for(common.RoadConnection rc : outlink2roadconnection.values())
            roadconnection2states.put(rc.getId(),new HashSet<>());

        // add all states
        for (KeyCommPathOrLink key : states) {
            Long outlink_id = key.isPath ? link.path2outlink.get(key.pathOrlink_id) :
                    key.pathOrlink_id;

            common.RoadConnection rc = get_roadconnection_for_outlink(outlink_id);
            if (rc!=null && roadconnection2states.containsKey(rc.getId()))
                roadconnection2states.get(rc.getId()).add(key);
        }

    }

    public void add_key(KeyCommPathOrLink state) {

        states.add(state);

        // state2roadconnection: for this state, what is the road connection exiting
        // this lanegroup that it will follow. There need not be one: this may not be
        // a target lane group for this state.

        // sink case -- no road connection
        if(link.is_sink){
            state2roadconnection.put(state,null);
            return;
        }

        // get next link according to the case
        Long next_link;
        if(link.end_node.is_many2one){
            next_link = link.end_node.out_links.values().iterator().next().getId();
        }
        else {
            if (state.isPath) {
                Path path = (Path) link.network.scenario.subnetworks.get(state.pathOrlink_id);
                next_link = path.get_link_following(link).getId();
            } else {
                next_link = state.pathOrlink_id;
            }
        }

        // store in map
        RoadConnection rc = get_roadconnection_for_outlink(next_link);
        if(rc!=null)
            state2roadconnection.put(state,rc.getId());

    }

    public Set<Long> get_dwn_links(){
        return outlink2roadconnection.keySet();
    }

    public boolean link_is_link_reachable(Long link_id){
        return outlink2roadconnection.containsKey(link_id);
    }

    // returns null if either the outlink is unknown or the lanegroup is one-to-one
    public RoadConnection get_roadconnection_for_outlink(Long link_id){
        return link_id==null? null : outlink2roadconnection.get(link_id);
    }

    public Set<AbstractLaneGroup> get_accessible_lgs_in_outlink(Link out_link){

        // if the end node is one to one, then all lanegroups in the next link are equally accessible
        if(link.end_node.is_many2one) {
            if (link.outlink2lanegroups.containsKey(out_link.getId()))
                return new HashSet<>(out_link.long_lanegroups.values());     // all downstream lanegroups are accessible
            else
                return null;
        }

        // otherwise, get the road connection connecting this lg to out_link
        RoadConnection rc = outlink2roadconnection.get(out_link.getId());

        // return lanegroups connected to by this road connection
        return out_link.get_unique_lanegroups_for_up_lanes(rc.end_link_from_lane,rc.end_link_to_lane);

    }

    public int get_num_exiting_road_connections(){
        return link.end_node.is_many2one ? 0 : roadconnection2states.size();
    }



}
