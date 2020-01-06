package profiles;

import commodity.Commodity;
import commodity.Subnetwork;
import common.Link;
import error.OTMErrorLog;
import error.OTMException;
import dispatch.Dispatcher;
import dispatch.EventSplitChange;
import common.Node;
import runner.Scenario;
import utils.OTMUtils;

import java.util.*;
import java.util.stream.Collectors;

public class SplitMatrixProfile {

    public long commodity_id;
    public Node node;
    public long link_in_id;

    // link out id -> split profile
    private Profile2D splits;

    ////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public SplitMatrixProfile(long commodity_id,Node node,long link_in_id,float start_time,Float dt) {
        this.commodity_id = commodity_id;
        this.node = node;
        this.link_in_id = link_in_id;
        this.splits = new Profile2D(start_time,dt);
    }

    public void validate(Scenario scenario,OTMErrorLog errorLog) {

        // TODO: Validate that you cannot have splits where there are no road connections.

        if( node==null )
            errorLog.addError("node==null");

        // commodity id is good
        Commodity commodity = scenario.commodities.get(commodity_id);
        if(commodity==null) {
            errorLog.addError("commodity==null)");
            return;
        }

        if(commodity.pathfull)
            errorLog.addWarning("Split ratios have been defined for pathfull commodity id=" + commodity_id);

        // node is within commodity subnetworks
//        Set<Node> subnetwork_nodes = new HashSet<>();
//        for(Subnetwork subnetwork : commodity.subnetworks){
//            subnetwork_nodes.addAll(subnetwork.links.stream().map(x->x.end_node).collect(Collectors.toSet()));
//            subnetwork_nodes.addAll(subnetwork.links.stream().map(x->x.start_node).collect(Collectors.toSet()));
//        }
//        if(!subnetwork_nodes.contains(node))
//            errorLog.addError("!subnetwork_nodes.contains(node))");

        // link_in_id is good
        Link link_in = scenario.network.links.get(link_in_id);
        if(link_in==null)
            errorLog.addError("link_in==null");

        // link_in_id is in a subnetwork
        for(Subnetwork subnetwork : commodity.subnetworks){
            if(!subnetwork.links.contains(link_in))
                errorLog.addError("!commodity.subnetwork.links.contains(link_in)");
        }

//        // road connections are good
//        if(!node.road_connections.stream().allMatch(rc -> rc.start_link!=null && rc.end_link!=null))
//            errorLog.addError(String.format("Split matrix profile on %d has a bad road connection",node.getId()));

        Set<Long> reachable_outlinks = node.road_connections.stream()
                .filter(rc->rc.start_link!=null && rc.end_link!=null && rc.start_link.getId().equals(link_in_id))
                .map(z->z.end_link.getId())
                .collect(Collectors.toSet());

        // check that there is a road connection for every split ratio
        if(!reachable_outlinks.containsAll(splits.values.keySet())) {
            Set<Long> unreachable = new HashSet<>();
            unreachable.addAll(splits.values.keySet());
            unreachable.removeAll(reachable_outlinks);
            errorLog.addError(String.format("No road connection supporting split from link %d to link(s) %s",link_in_id, OTMUtils.comma_format(unreachable)));
        }

        splits.validate(errorLog,node.getId(),link_in_id,commodity_id);

    }

    public void initialize(float now) throws OTMException {
        Map<Long,Double> value = splits.get_value_for_time(now);
        node.send_splits_to_inlinks(commodity_id,link_in_id,value);
    }

    public void add_split(jaxb.Split jaxb_split) throws OTMException{
        long linkout_id = jaxb_split.getLinkOut();
        if(splits.have_key(linkout_id))
            throw new OTMException("Repeated link out");
        splits.add_entry(linkout_id,  jaxb_split.getContent() );
    }

    public void add_split(Long link_outid,Double value) throws OTMException {
        value = Math.max(value,0d);
        value = Math.min(value,1d);
        splits.add_entry(link_outid,value);
    }

    public void register_with_dispatcher(Dispatcher dispatcher) {
        Map<Long,Double> time_splits = splits.get_value_for_time(dispatcher.current_time);
        dispatcher.register_event(new EventSplitChange(dispatcher,dispatcher.current_time, this, time_splits));
    }

    ///////////////////////////////////////////
    // public
    ///////////////////////////////////////////

    public void register_next_change(Dispatcher dispatcher,float timestamp){
        TimeMap time_map = splits.get_change_following(timestamp);
        if(time_map!=null)
            dispatcher.register_event(new EventSplitChange(dispatcher,time_map.time, this, time_map.value));
    }

    public float get_dt(){
        return splits.dt;
    }

    public float get_start_time(){
        return splits.start_time;
    }

    public Map<Long,List<Double>> get_outlink_to_profile(){
        return splits.values;
    }

    public Profile2D clone_splits(){
        return splits.clone();
    }

}
