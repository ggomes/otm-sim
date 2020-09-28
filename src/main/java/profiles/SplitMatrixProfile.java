package profiles;

import commodity.Commodity;
import commodity.Subnetwork;
import common.Link;
import error.OTMErrorLog;
import error.OTMException;
import dispatch.Dispatcher;
import dispatch.EventSplitChange;
import common.Node;
import common.Scenario;
import utils.OTMUtils;

import java.util.*;
import java.util.stream.Collectors;

public class SplitMatrixProfile {

    public long commodity_id;
    public Link link_in;
    public Profile2D splits;   // link out id -> split profile

    // current splits
    public Map<Long,Double> outlink2split;        // output link id -> split
    public List<LinkCumSplit> link_cumsplit;      // output link id -> cummulative split

    ////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public SplitMatrixProfile(long commodity_id,Link link_in) {
        this.commodity_id = commodity_id;
        this.link_in = link_in;
    }

    public void validate(Scenario scenario,OTMErrorLog errorLog) {

        // TODO: Validate that you cannot have splits where there are no road connections.

        Node node = link_in.end_node;

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

        // link_in_id is good
        if(link_in==null)
            errorLog.addError("link_in==null");

        // link_in_id is in a subnetwork
        for(Subnetwork subnetwork : commodity.subnetworks){
            if(!subnetwork.get_links().contains(link_in))
                errorLog.addError("!commodity.subnetwork.links.contains(link_in)");
        }

        Set<Long> reachable_outlinks = node.road_connections.stream()
                .filter(rc->rc.start_link!=null && rc.end_link!=null && rc.start_link.getId().equals(link_in.getId()))
                .map(z->z.end_link.getId())
                .collect(Collectors.toSet());

        // check that there is a road connection for every split ratio
        if(!reachable_outlinks.containsAll(splits.values.keySet())) {
            Set<Long> unreachable = new HashSet<>();
            unreachable.addAll(splits.values.keySet());
            unreachable.removeAll(reachable_outlinks);
            errorLog.addError(String.format("No road connection supporting split from link %d to link(s) %s",link_in.getId(), OTMUtils.comma_format(unreachable)));
        }

        splits.validate(errorLog);

    }

    public void initialize(Dispatcher dispatcher) throws OTMException {
        float now = dispatcher.current_time;
        this.set_splits(splits.get_value_for_time(now));
        Map<Long,Double> time_splits = splits.get_value_for_time(now);
        dispatcher.register_event(new EventSplitChange(dispatcher,now, this, time_splits));
    }

//    public void add_split(Long link_outid,Double value) throws OTMException {
//        value = Math.max(value,0d);
//        value = Math.min(value,1d);
//        splits.add_entry(link_outid,value);
//    }

    ////////////////////////
    // get current values
    ////////////////////////

    // return an output link id according to split ratios for this commodity and line
    public Long sample_output_link(){

        double r = OTMUtils.random_zero_to_one();

        Optional<LinkCumSplit> z = link_cumsplit.stream()
                .filter(x->x.cumsplit<r)  // get all cumsplit < out
                .reduce((a,b)->b);        // get last such vauue

        return z.isPresent() ? z.get().link_id : null;
    }

    ///////////////////////////////////////////
    // used by EventSplitChange
    ///////////////////////////////////////////

    public void set_splits(Map<Long,Double> outlink2split) {

        this.outlink2split = outlink2split;

        if(outlink2split.size()<=1)
            return;

        double s = 0d;
        link_cumsplit = new ArrayList<>();
        for(Map.Entry<Long,Double> e : outlink2split.entrySet()){
            link_cumsplit.add(new LinkCumSplit(e.getKey(),s));
            s += e.getValue();
        }
    }

    public void register_next_change(Dispatcher dispatcher,float timestamp){
        TimeMap time_map = splits.get_change_following(timestamp);
        if(time_map!=null)
            dispatcher.register_event(new EventSplitChange(dispatcher,time_map.time, this, time_map.value));
    }

    ///////////////////////////////////////////
    // public API
    ///////////////////////////////////////////

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

    ///////////////////////////////////////////
    // class
    ///////////////////////////////////////////

    private class LinkCumSplit{
        public Long link_id;
        public Double cumsplit;
        public LinkCumSplit(Long link_id, Double cumsplit) {
            this.link_id = link_id;
            this.cumsplit = cumsplit;
        }
    }
}
