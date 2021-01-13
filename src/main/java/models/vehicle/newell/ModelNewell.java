package models.vehicle.newell;

import core.AbstractVehicle;
import core.Link;
import core.RoadConnection;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import dispatch.Pokable;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.OutputRequest;
import core.AbstractLaneGroup;
import core.AbstractVehicleModel;
import output.AbstractOutput;
import output.InterfaceVehicleListener;
import core.Scenario;
import utils.StochasticProcess;

import java.util.*;

public class ModelNewell extends AbstractVehicleModel implements Pokable {

    public final float dt;

    public ModelNewell(String name, Set<Link> links, StochasticProcess process, jaxb.ModelParams params) throws OTMException {
        super(name,links,process);
        this.dt = params.getSimDt()==null ? -1 : params.getSimDt();
    }

    //////////////////////////////////////////////////////////////
    // InterfaceModel
    //////////////////////////////////////////////////////////////

    @Override
    public AbstractOutput create_output(Scenario scenario, String prefix, String output_folder, OutputRequest jaxb_or)  throws OTMException {
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
    public AbstractLaneGroup create_lane_group(Link link, core.geometry.Side side, Float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs,jaxb.Roadparam rp) {
        return new NewellLaneGroup(link,side,length,num_lanes,start_lane,out_rcs,rp);
    }

    @Override
    public Map<AbstractLaneGroup, Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups) {
        return std_lanegroup_proportions(candidate_lanegroups);
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
    public void set_state_for_link(Link link) {

    }

    @Override
    public void validate_pre_init(OTMErrorLog errorLog) {

    }


    @Override
    public void validate_post_init(OTMErrorLog errorLog) {

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
            for (AbstractLaneGroup alg : link.get_lgs()) {
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
            for (AbstractLaneGroup alg : link.get_lgs()) {
                NewellLaneGroup lg = (NewellLaneGroup) alg;
                Iterator<NewellVehicle> it = lg.vehicles.iterator();
                while (it.hasNext()) {
                    NewellVehicle vehicle = it.next();
                    // possibly release the vehicle from this lanegroup
                    if (vehicle.new_pos > lg.get_length()) {
                        boolean released = lg.release_vehicle(timestamp, it, vehicle);

                        if(!released)
                            vehicle.new_pos = (vehicle.pos + lg.get_length())/2d;

                    }
                }
            }
        }

        // update position
        for(Link link : links) {
            for (AbstractLaneGroup alg : link.get_lgs()) {
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
            for (AbstractLaneGroup alg : link.get_lgs()) {
                NewellLaneGroup lg = (NewellLaneGroup) alg;
                Iterator<NewellVehicle> it = lg.vehicles.iterator();
                while (it.hasNext()) {
                    NewellVehicle vehicle = it.next();
                    if(vehicle.leader==null) {

                        if(vehicle.get_next_link_id()==null)
                            vehicle.headway = Double.POSITIVE_INFINITY;
                        else{

                            Collection<AbstractLaneGroup> next_lgs = link.get_network().links.get(vehicle.get_next_link_id()).get_lgs();
                            OptionalDouble next_vehicle_position = next_lgs.stream()
                                    .mapToDouble(x->x.get_upstream_vehicle_position())
                                    .min();

                            if( !next_vehicle_position.isPresent() || Double.isNaN(next_vehicle_position.getAsDouble()) ){
                                vehicle.headway = Double.POSITIVE_INFINITY;
                            } else {
                                vehicle.headway = next_vehicle_position.getAsDouble() + vehicle.get_lanegroup().get_length() - vehicle.pos;
                            }

                        }
                    }
                    else{
                        if(vehicle.leader.get_lanegroup()==vehicle.get_lanegroup())
                            vehicle.headway = vehicle.leader.pos - vehicle.pos;
                        else
                            vehicle.headway = vehicle.leader.pos - vehicle.pos + vehicle.get_lanegroup().get_length();
                    }
                }

                lg.update_long_supply();
            }
        }

    }

}
