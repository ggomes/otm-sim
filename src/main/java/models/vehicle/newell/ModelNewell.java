package models.vehicle.newell;

import common.AbstractVehicle;
import common.Link;
import common.RoadConnection;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import dispatch.Pokable;
import error.OTMErrorLog;
import error.OTMException;
import geometry.FlowPosition;
import geometry.Side;
import jaxb.OutputRequest;
import common.AbstractLaneGroup;
import models.vehicle.AbstractVehicleModel;
import output.AbstractOutput;
import output.InterfaceVehicleListener;
import output.animation.AbstractLinkInfo;
import common.Scenario;
import utils.StochasticProcess;

import java.util.*;

public class ModelNewell extends AbstractVehicleModel implements Pokable {

    public float dt;

    public ModelNewell(String name, boolean is_default, StochasticProcess process, jaxb.ModelParams param) {
        super(name, is_default,process);
        this.dt = param.getSimDt()==null ? -1 : param.getSimDt();
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
//        switch (jaxb_or.getQuantity()) {
//            case "trajectories":
//                Float outDt = jaxb_or.getDt();
//                output = new NewellTrajectories(scenario, this,prefix, output_folder, outDt);
//                break;
//            default:
//                throw new OTMException("Bad output identifier : " + jaxb_or.getQuantity());
//        }
        return output;
    }

    @Override
    public AbstractLaneGroup create_lane_group(Link link, Side side, FlowPosition flwpos, Float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs,jaxb.Roadparam rp) {
        return new NewellLaneGroup(link,side,flwpos,length,num_lanes,start_lane,out_rcs,rp);
    }

    @Override
    public Map<AbstractLaneGroup, Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups) {
        return std_lanegroup_proportions(candidate_lanegroups);
    }

    @Override
    public AbstractLinkInfo get_link_info(Link link) {
        return null;
    }

    //////////////////////////////////////////////////////////////
    // InterfaceVehicleModel
    //////////////////////////////////////////////////////////////

    @Override
    public AbstractVehicle translate_vehicle(AbstractVehicle that){
        if(that instanceof NewellVehicle)
            return that;
        else
            return new NewellVehicle(that);
    }

    @Override
    public AbstractVehicle create_vehicle(Long comm_id,Set<InterfaceVehicleListener> event_listeners) {
        return new NewellVehicle(comm_id,event_listeners);
    }

    //////////////////////////////////////////////////
    // Completions from AbstractModel
    //////////////////////////////////////////////////

    @Override
    public void reset(Link link) {

    }

    @Override
    public void register_with_dispatcher(Scenario scenario, Dispatcher dispatcher, float start_time) {
        dispatcher.register_event(new EventPoke(dispatcher, 60,start_time + dt, this));
    }

    //////////////////////////////////////////////////
    // State update (Pokable)
    //////////////////////////////////////////////////

    @Override
    public void poke(Dispatcher dispatcher, float timestamp) throws OTMException {
        update_state(timestamp);
        dispatcher.register_event(new EventPoke(dispatcher, 6,timestamp + dt, this));
    }

    private void update_state(float timestamp) throws OTMException{

        // apply Newell's update formula to all vehicles
        for(Link link : links) {
            for (AbstractLaneGroup alg : link.lanegroups_flwdn.values()) {
                NewellLaneGroup lg = (NewellLaneGroup) alg;
                for( NewellVehicle vehicle : lg.vehicles ) {
                    double dx = Math.min(lg.dv, vehicle.headway - lg.dw);
                    dx = Math.min( dx , vehicle.headway * lg.dc);
                    dx = Math.max( dx , 0d );
                    vehicle.new_pos = vehicle.pos + dx;
                }
            }
        }

        // move vehicles to new link
        for(Link link : links) {
            for (AbstractLaneGroup alg : link.lanegroups_flwdn.values()) {
                NewellLaneGroup lg = (NewellLaneGroup) alg;
                Iterator<NewellVehicle> it = lg.vehicles.iterator();
                while (it.hasNext()) {
                    NewellVehicle vehicle = it.next();
                    // possibly release the vehicle from this lanegroup
                    if (vehicle.new_pos > lg.length) {
                        boolean released = lg.release_vehicle(timestamp, it, vehicle);

                        if(!released)
                            vehicle.new_pos = (vehicle.pos + lg.length)/2d;

                    }
                }
            }
        }

        // update position
        for(Link link : links) {
            for (AbstractLaneGroup alg : link.lanegroups_flwdn.values()) {
                NewellLaneGroup lg = (NewellLaneGroup) alg;
                Iterator<NewellVehicle> it = lg.vehicles.iterator();
                while (it.hasNext()) {
                    NewellVehicle vehicle = it.next();
                    vehicle.pos = vehicle.new_pos;
                }
            }
        }

        // update headway
        for(Link link : links) {
            for (AbstractLaneGroup alg : link.lanegroups_flwdn.values()) {
                NewellLaneGroup lg = (NewellLaneGroup) alg;
                Iterator<NewellVehicle> it = lg.vehicles.iterator();
                while (it.hasNext()) {
                    NewellVehicle vehicle = it.next();
                    if(vehicle.leader==null) {

                        if(vehicle.get_next_link_id()==null)
                            vehicle.headway = Double.POSITIVE_INFINITY;
                        else{

                            Collection<AbstractLaneGroup> next_lgs = link.network.links.get(vehicle.get_next_link_id()).lanegroups_flwdn.values();
                            OptionalDouble next_vehicle_position = next_lgs.stream()
                                    .mapToDouble(x->x.get_upstream_vehicle_position())
                                    .min();

                            if( !next_vehicle_position.isPresent() || Double.isNaN(next_vehicle_position.getAsDouble()) ){
                                vehicle.headway = Double.POSITIVE_INFINITY;
                            } else {
                                vehicle.headway = next_vehicle_position.getAsDouble() + vehicle.get_lanegroup().length - vehicle.pos;
                            }

                        }
                    }
                    else{
                        if(vehicle.leader.get_lanegroup()==vehicle.get_lanegroup())
                            vehicle.headway = vehicle.leader.pos - vehicle.pos;
                        else
                            vehicle.headway = vehicle.leader.pos - vehicle.pos + vehicle.get_lanegroup().length;
                    }
                }

                lg.update_supply();
            }
        }

    }

}
