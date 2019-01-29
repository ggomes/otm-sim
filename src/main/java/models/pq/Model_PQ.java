package models.pq;

import common.AbstractVehicle;
import common.RoadConnection;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import geometry.FlowDirection;
import geometry.Side;
import jaxb.OutputRequest;
import models.*;
import common.Link;
import error.OTMException;
import output.AbstractOutput;
import output.InterfaceVehicleListener;
import output.animation.AbstractLinkInfo;
import runner.Scenario;

import java.util.*;

public class Model_PQ extends AbstractVehicleModel {

    public Model_PQ(String name,boolean is_default) {
        super(name,is_default);
    }

    //////////////////////////////////////////////////
    // load
    //////////////////////////////////////////////////

    @Override
    public void set_road_param(Link link,jaxb.Roadparam r) {
        super.set_road_param(link,r);
        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values())
            lg.set_road_params(r);
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
    }

    @Override
    public void reset(Link link) {
    }

    @Override
    public void build() {
    }

    //////////////////////////////////////////////////
    // factory
    //////////////////////////////////////////////////

    @Override
    public AbstractOutput create_output_object(Scenario scenario, String prefix, String output_folder, OutputRequest jaxb_or)  throws OTMException {
        AbstractOutput output = null;
        switch (jaxb_or.getQuantity()) {
            case "queues":
                Long commodity_id = jaxb_or.getCommodity();
                Float outDt = jaxb_or.getDt();
                output = new OutputQueues(scenario, this,prefix, output_folder, commodity_id, outDt);
                break;
            default:
                throw new OTMException("Bad output identifier : " + jaxb_or.getQuantity());
        }
        return output;
    }

    @Override
    public AbstractLaneGroup create_lane_group(Link link, Side side, FlowDirection flowdir, Float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs) {
        return new models.pq.LaneGroup(link,side,flowdir,length,num_lanes,start_lane,out_rcs);
    }

    @Override
    public AbstractVehicle create_vehicle(Long comm_id,Set<InterfaceVehicleListener> event_listeners) {
        return new models.pq.Vehicle(comm_id,event_listeners);
    }

    @Override
    public AbstractVehicle translate_vehicle(AbstractVehicle that){
        if(that instanceof models.pq.Vehicle)
            return that;
        else
            return new models.pq.Vehicle(that);
    }

    //////////////////////////////////////////////////
    // run
    //////////////////////////////////////////////////

    @Override
    public void register_with_dispatcher(Scenario scenario, Dispatcher dispatcher, float start_time) {
    }

    @Override
    public AbstractLinkInfo get_link_info(Link link) {
        return new output.animation.meso.LinkInfo(link);
    }

    //////////////////////////////////////////////////////////////
    // protected
    //////////////////////////////////////////////////////////////

    protected void process_lane_change_request(Link link,float timestamp,LaneChangeRequest x) throws OTMException {

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
