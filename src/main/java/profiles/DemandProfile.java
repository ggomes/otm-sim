package profiles;

import commodity.Commodity;
import commodity.Path;
import commodity.Subnetwork;
import common.Network;
import error.OTMErrorLog;
import error.OTMException;
import dispatch.Dispatcher;
import dispatch.EventDemandChange;
import common.Link;
import keys.DemandType;
import keys.KeyCommodityDemandTypeId;
import models.SourceVehicle;
import runner.Scenario;
import utils.OTMUtils;

import java.util.List;

public class DemandProfile extends AbstractDemandProfile {

    public Link link;
    public Path path;

    ////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public DemandProfile(Subnetwork subnetwork,Commodity commodity,float start_time,float dt,List<Double> values) throws OTMException{
        if(subnetwork==null)
            throw new OTMException("bad path.");
        if(commodity==null)
            throw new OTMException("Bad commodity");
        if(!(subnetwork instanceof Path))
            throw new OTMException("Subnetwork is not a path");
        create_pathfull_demand((Path)subnetwork,commodity,start_time,dt,values);
    }

    public DemandProfile(jaxb.Demand jd, Network network) throws OTMException {
        Commodity comm = network.scenario.commodities.get(jd.getCommodityId());
        if(comm==null)
            throw new OTMException("Bad commodity in demands");
        if(comm.pathfull){
            if(jd.getSubnetwork()==null)
                throw new OTMException("Subnetwork not specified in demand profile for commodity " + comm.getId());

            Subnetwork subnetwork = network.scenario.subnetworks.get(jd.getSubnetwork());
            if(subnetwork==null)
                throw new OTMException("Bad subnetwork id (" + jd.getSubnetwork() + ") in demand for commodity " + comm.getId());

            if(!(subnetwork instanceof Path))
                throw new OTMException("Subnetwork is not a path: id " + jd.getSubnetwork() + ", in demand for commodity " + comm.getId());

            create_pathfull_demand((Path)subnetwork,comm,jd.getStartTime(), jd.getDt(), OTMUtils.csv2list(jd.getContent()));
        } else {
            if(jd.getLinkId()==null)
                throw new OTMException("Link not specified in demand profile for commodity " + comm.getId());

            Link link = network.links.get(jd.getLinkId());
            if(link==null)
                throw new OTMException("Bad link id (" + jd.getLinkId() + ") in demand for commodity " + comm.getId());

            create_pathless_demand(link,comm,jd.getStartTime(), jd.getDt(),OTMUtils.csv2list(jd.getContent()));
        }
    }

    ////////////////////////////////////////////
    // implementation
    ///////////////////////////////////////////

    @Override
    public void validate(OTMErrorLog errorLog) {

        if( (link==null && path==null) || (link!=null && path!=null) )
            errorLog.addError("(link==null && path==null) || (link!=null && path!=null)");

        if(link!=null){

            // the commodity should be pathless
            if(commodity.pathfull)
                errorLog.addError("demand for pathfull commodity id=" + commodity.getId() + ", specifies an origin link instead of a subnetwork.");

            // link is a source
            if (!link.is_source)
                errorLog.addError(String.format("In demand for commodity %d, link %d is not a source.",this.get_commodity_id(),link.getId()));

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

            // this should be a commodity subnetwork
            if(!commodity.subnetworks.stream().anyMatch(x->x.is_global))
                if(!commodity.subnetworks.stream().anyMatch(x->x.getId().equals(path.getId())))
                    errorLog.addError("in demand profile for commodity " + commodity.getId() + ", subnetwork " + path.getId() + " is not allowed.");
        }

        // commodity_id exists
        if (commodity== null)
            errorLog.addError("bad commodity in demand");

        // profile
        profile.validate(errorLog);

        // source
        source.validate(errorLog);

    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
//        float now = scenario.get_current_time();
//        double value = profile.get_value_for_time(now);
//        source.set_demand_in_veh_per_timestep(scenario.dispatcher,now,value*scenario.sim_dt);
    }

    @Override
    public void register_with_dispatcher(Dispatcher dispatcher) {
        double value = profile.get_value_for_time(dispatcher.current_time);
        dispatcher.register_event(new EventDemandChange(dispatcher,dispatcher.current_time,this,value));
    }

    @Override
    public Long get_origin_node_id() {
        return path==null ? link.start_node.getId() : path.get_origin_node_id();
    }

    @Override
    public Long get_destination_node_id() {
        return path==null ? null : path.get_destination_node_id();
    }

    ////////////////////////////////////////////
    // public
    ///////////////////////////////////////////

    public void register_next_change(Dispatcher dispatcher,float timestamp) {
        TimeValue time_value = profile.get_change_following(timestamp);
        if (time_value != null) {
            dispatcher.register_event(new EventDemandChange(dispatcher, time_value.time, this, time_value.value));

            // schedule next vehicle
            if(source instanceof SourceVehicle)
                ((SourceVehicle)source).schedule_next_vehicle(dispatcher,timestamp);
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

    public KeyCommodityDemandTypeId get_key(){
        return new KeyCommodityDemandTypeId(get_commodity_id(),get_link_or_path_id(),get_type());
    }

    public Long get_link_or_path_id(){
        if(link!=null)
            return link.getId();
        if(path!=null)
            return path.getId();
        return null;
    }

    public Link get_origin(){
        if(link!=null)
            return link;
        if(path!=null)
            return path.get_origin();
        return null;
    }

    ////////////////////////////////////////////
    // private
    ///////////////////////////////////////////

    private void create_pathless_demand(Link link,Commodity commodity,float start_time,Float dt,List<Double> values) throws OTMException {
        this.link = link;
        this.path = null;
        this.commodity = commodity;

        // create a source and add it to the origin
        Link origin = get_origin();
        source = origin.model.create_source(origin,this,commodity,null);
        origin.sources.add(source);

        // assume the content to be given in veh/hr
        profile = new Profile1D(start_time,dt,values);
        profile.multiply(1.0/3600.0);
    }

    private void create_pathfull_demand(Path path,Commodity commodity,float start_time,Float dt,List<Double> values) throws OTMException {
        this.link = null;
        this.path = path;
        this.commodity = commodity;

        // create a source and add it to the origin
        Link origin = get_origin();
        source = origin.model.create_source(origin,this,commodity,path);
        origin.sources.add(source);

        // assume the content to be given in veh/hr
        profile = new Profile1D(start_time,dt,values);
        profile.multiply(1.0/3600.0);
    }

}