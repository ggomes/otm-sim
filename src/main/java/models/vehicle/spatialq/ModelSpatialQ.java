package models.vehicle.spatialq;

import common.AbstractVehicle;
import common.RoadConnection;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import geometry.FlowPosition;
import geometry.Side;
import jaxb.OutputRequest;
import models.*;
import common.Link;
import error.OTMException;
import models.vehicle.AbstractVehicleModel;
import output.AbstractOutput;
import output.InterfaceVehicleListener;
import output.animation.AbstractLinkInfo;
import runner.Scenario;
import utils.StochasticProcess;

import java.util.*;
import java.util.stream.Collectors;

public class ModelSpatialQ extends AbstractVehicleModel {

    public ModelSpatialQ(String name, boolean is_default, StochasticProcess process, jaxb.ModelParams param) {
        super(name,is_default,process);
    }

    //////////////////////////////////////////////////////////////
    // InterfaceModel
    //////////////////////////////////////////////////////////////

    @Override
    public void validate(OTMErrorLog errorLog) {
    }

    @Override
    public AbstractOutput create_output_object(Scenario scenario, String prefix, String output_folder, OutputRequest jaxb_or)  throws OTMException {
        AbstractOutput output = null;
        switch (jaxb_or.getQuantity()) {
            case "queues":
                Long commodity_id = jaxb_or.getCommodity();
                Float outDt = jaxb_or.getDt();
                output = new OutputQueues(scenario,prefix, output_folder, commodity_id, links.stream().map(x->x.getId()).collect(Collectors.toList()), outDt);
                break;
            default:
                throw new OTMException("Bad output identifier : " + jaxb_or.getQuantity());
        }
        return output;
    }

    @Override
    public AbstractLaneGroup create_lane_group(Link link, Side side, FlowPosition flwpos, Float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs) {
        return new LaneGroup(link,side,flwpos,length,num_lanes,start_lane,out_rcs);
    }

    @Override
    public Map<AbstractLaneGroup, Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups) {
        return std_lanegroup_proportions(candidate_lanegroups);
    }

    @Override
    public AbstractLinkInfo get_link_info(Link link) {
        return new output.animation.meso.LinkInfo(link);
    }

    //////////////////////////////////////////////////////////////
    // Completions from AbstractModel
    //////////////////////////////////////////////////////////////

    @Override
    public void set_road_param(Link link,jaxb.Roadparam r) {
        super.set_road_param(link,r);
    }

    @Override
    public void register_with_dispatcher(Scenario scenario, Dispatcher dispatcher, float start_time) {
    }

    //////////////////////////////////////////////////////////////
    // InterfaceVehicleModel
    //////////////////////////////////////////////////////////////

    @Override
    public AbstractVehicle translate_vehicle(AbstractVehicle that){
        if(that instanceof Vehicle)
            return that;
        else
            return new Vehicle(that);
    }

    @Override
    public AbstractVehicle create_vehicle(Long comm_id,Set<InterfaceVehicleListener> event_listeners) {
        return new Vehicle(comm_id,event_listeners);
    }

    //////////////////////////////////////////////////////////////
    // static protected
    //////////////////////////////////////////////////////////////

    static protected void process_lane_change_request(Link link,float timestamp,LaneChangeRequest x) throws OTMException {

        if(x==null)
            return;

        // the vehicle must be in to_queue
        if(x.requester.my_queue!=x.from_queue)
            return;

        // move the vehicle to the destination queue
        x.requester.move_to_queue(timestamp,x.to_queue);

        // remove all of its requests by this vehicle in this link
        for (AbstractLaneGroup lanegroup : link.lanegroups_flwdn.values()) {
            LaneGroup lg = (LaneGroup) lanegroup;
            lg.transit_queue.remove_lane_change_requests_for_vehicle(x.requester);
            lg.waiting_queue.remove_lane_change_requests_for_vehicle(x.requester);
        }

        // vehicle is not longer changing lanes
        x.requester.waiting_for_lane_change = false;

    }

}
