package models.vehicle.spatialq;

import core.*;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import jaxb.OutputRequest;
import error.OTMException;
import core.AbstractVehicleModel;
import output.AbstractOutput;
import output.InterfaceVehicleListener;
import utils.StochasticProcess;

import java.util.*;
import java.util.stream.Collectors;

public class ModelSpatialQ extends AbstractVehicleModel {

    public ModelSpatialQ(String name, Set<Link> links,  StochasticProcess process) throws OTMException {
        super(name,links,process);
    }

    //////////////////////////////////////////////////////////////
    // InterfaceModel
    //////////////////////////////////////////////////////////////

    @Override
    public void validate(OTMErrorLog errorLog) {
    }

    @Override
    public AbstractOutput create_output(Scenario scenario, String prefix, String output_folder, OutputRequest jaxb_or)  throws OTMException {
        AbstractOutput output = null;
        switch (jaxb_or.getQuantity()) {
            case "queues":
                Long commodity_id = jaxb_or.getCommodity();
                Float outDt = jaxb_or.getDt();
                output = new OutputLinkQueues(scenario,prefix, output_folder, commodity_id, links.stream().map(x->x.getId()).collect(Collectors.toList()), outDt);
                break;
            default:
                throw new OTMException("Bad output identifier : " + jaxb_or.getQuantity());
        }
        return output;
    }

    @Override
    public AbstractLaneGroup create_lane_group(Link link, core.geometry.Side side, Float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs,jaxb.Roadparam rp) {
        return new MesoLaneGroup(link,side,length,num_lanes,start_lane,out_rcs,rp);
    }

    @Override
    public Map<AbstractLaneGroup, Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups) {
        return std_lanegroup_proportions(candidate_lanegroups);
    }

    //////////////////////////////////////////////////////////////
    // Completions from AbstractModel
    //////////////////////////////////////////////////////////////

    @Override
    public void set_state_for_link(Link link) {

    }

    @Override
    public void register_with_dispatcher(Scenario scenario, Dispatcher dispatcher, float start_time) {
    }

    //////////////////////////////////////////////////////////////
    // InterfaceVehicleModel
    //////////////////////////////////////////////////////////////

    @Override
    public AbstractVehicle translate_vehicle(AbstractVehicle that){
        if(that instanceof MesoVehicle)
            return that;
        else
            return new MesoVehicle(that);
    }

    @Override
    public AbstractVehicle create_vehicle(Long comm_id,Set<InterfaceVehicleListener> event_listeners) {
        return new MesoVehicle(comm_id,event_listeners);
    }

    //////////////////////////////////////////////////////////////
    // static protected
    //////////////////////////////////////////////////////////////

//    static protected void process_lane_change_request(Link link,float timestamp,LaneChangeRequest x) throws OTMException {
//
//        if(x==null)
//            return;
//
//        // the vehicle must be in to_queue
//        if(x.requester.my_queue!=x.from_queue)
//            return;
//
//        // move the vehicle to the destination queue
//        x.requester.move_to_queue(timestamp,x.to_queue);
//
//        // remove all of its requests by this vehicle in this link
//        for (AbstractLaneGroup lanegroup : link.lanegroups_flwdn.values()) {
//            MesoLaneGroup lg = (MesoLaneGroup) lanegroup;
//            lg.transit_queue.remove_lane_change_requests_for_vehicle(x.requester);
//            lg.waiting_queue.remove_lane_change_requests_for_vehicle(x.requester);
//        }
//
//        // vehicle is not longer changing lanes
//        x.requester.waiting_for_lane_change = false;
//
//    }

}
