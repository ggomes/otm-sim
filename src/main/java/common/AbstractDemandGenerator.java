package common;

import commodity.Commodity;
import commodity.Path;
import dispatch.Dispatcher;
import dispatch.EventDemandChange;
import error.OTMErrorLog;
import error.OTMException;
import commodity.DemandType;
import models.vehicle.VehicleDemandGenerator;
import profiles.Profile1D;
import profiles.TimeValue;

public abstract class AbstractDemandGenerator {

    // TODO: The child classes for this class are Fluid vs. Vehicle, but could also be
    // Pathfull vs. Pathless. Pathfull can be speeded up by caching the candidate lane groups,
    // which do not change. This is already done in the fluid source.

//    public DemandProfile profile;   // profile that created this source
    public Profile1D profile;
    public Link link;
    public Path path;
    public Commodity commodity;

    // demand value
    protected double source_demand_vps;    // vps

    public AbstractDemandGenerator(Link link, Profile1D profile, Commodity commodity, Path path){
        this.link = link;
        this.profile = profile;
        this.commodity = commodity;
        this.path = path;
        this.source_demand_vps = 0f;
    }

    public void delete(){
        link = null;
        profile = null;
    }

    public void set_demand_vps(Dispatcher dispatcher, float time, double vps) throws OTMException {
        source_demand_vps = vps;
    }

    public final State sample_key(){
        if(commodity.pathfull){
            return new State(commodity.getId(),path.getId(),true);
        } else {
            Long next_link_id = link.split_profile.get(commodity.getId()).sample_output_link();
            return new State(commodity.getId(),next_link_id,false);
        }
    }

    // use with caution. This simply adds ulgs all of the numbers in the profile.
    // It does not account for start time and end time.
    public double get_total_trips(){
        return profile==null ? 0d : profile.values.stream()
                .reduce(0.0, Double::sum)
                * profile.dt;
    }


    public void validate(OTMErrorLog errorLog) {

        if( (link==null && path==null) || (link!=null && path!=null) )
            errorLog.addError("(link==null && path==null) || (link!=null && path!=null)");

        if(link!=null){

            // the commodity should be pathless
            if(commodity.pathfull)
                errorLog.addError("demand for pathfull commodity id=" + commodity.getId() + ", specifies an origin link instead of a subnetwork.");

            // link is a source
            if (!link.is_source)
                errorLog.addError(String.format("In demand for commodity %d, link %d is not a source.",commodity.getId(),link.getId()));

            // this link is in the commodities subnetworks
//            if(!commodity.subnetworks.stream().anyMatch(x->x.links.contains(link)))
//                errorLog.addError("demand defined for commodity " + commodity.getId() + " on link " + link.getId());
        }

        if(path!=null){

            // the commodity should be pathless
            if(!commodity.pathfull)
                errorLog.addError("demand for pathless commodity id=" + commodity.getId() + ", specifies a subnetwork instead of an origin link.");

            // the path should be a path
            for(int i=0;i<path.ordered_links.size()-1;i++){
                Link this_link = path.ordered_links.get(i);
                Link next_link = path.ordered_links.get(i+1);
                if(!this_link.outlink2lanegroups.containsKey(next_link.getId()))
                    errorLog.addError("In path " + path.getId() + ", link " + next_link.getId() + " is not reachable from link " + this_link.getId());
            }

//            // this should be a commodity subnetwork
//            if(!commodity.subnetworks.stream().anyMatch(x->x.is_global))
//                if(!commodity.subnetworks.stream().anyMatch(x->x.getId().equals(path.getId())))
//                    errorLog.addError("in demand profile for commodity " + commodity.getId() + ", subnetwork " + path.getId() + " is not allowed.");
        }

        // commodity_id exists
        if (commodity== null)
            errorLog.addError("bad commodity in demand");

        // profile
        profile.validate(errorLog);

    }

    public void initialize(Scenario scenario) throws OTMException {
        float now = scenario.get_current_time();
        double value = profile.get_value_for_time(now);
        set_demand_vps(scenario.dispatcher,now,value);
        register_with_dispatcher(scenario.dispatcher);
    }

    public void register_with_dispatcher(Dispatcher dispatcher) {
        double value = profile.get_value_for_time(dispatcher.current_time);
        dispatcher.register_event(new EventDemandChange(dispatcher,dispatcher.current_time,this,value));
    }

    public Long get_origin_node_id() {
        return path==null ? link.start_node.getId() : path.get_origin_node_id();
    }

    public Long get_destination_node_id() {
        return path==null ? null : path.get_destination_node_id();
    }


    public void register_next_change(Dispatcher dispatcher,float timestamp) {
        TimeValue time_value = profile.get_change_following(timestamp);
        if (time_value != null) {
            dispatcher.register_event(new EventDemandChange(dispatcher, time_value.time, this, time_value.value));

            // schedule next vehicle
            if(this instanceof VehicleDemandGenerator)
                ((VehicleDemandGenerator)this).schedule_next_vehicle(dispatcher,timestamp);
        }
    }

    public DemandType get_type(){
        if(link!=null)
            return DemandType.pathless;
        if(path!=null)
            return DemandType.pathfull;
        return null;
    }

    public Long get_commodity_id(){
        return commodity.getId();
    }

//    public KeyCommodityDemandTypeId get_key(){
//        return new KeyCommodityDemandTypeId(get_commodity_id(),get_link_or_path_id(),get_type());
//    }

    public Long get_link_or_path_id(){
        if(link!=null)
            return link.getId();
        if(path!=null)
            return path.getId();
        return null;
    }


}
