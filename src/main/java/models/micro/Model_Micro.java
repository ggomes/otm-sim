package models.micro;

import common.AbstractVehicle;
import common.Link;
import common.RoadConnection;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import dispatch.InterfacePokable;
import error.OTMErrorLog;
import error.OTMException;
import geometry.FlowDirection;
import geometry.Side;
import jaxb.OutputRequest;
import models.AbstractLaneGroup;
import models.AbstractVehicleModel;
import output.AbstractOutput;
import output.InterfaceVehicleListener;
import output.animation.AbstractLinkInfo;
import runner.Scenario;

import java.util.*;

public class Model_Micro extends AbstractVehicleModel implements InterfacePokable {

    public float dt;

    public Model_Micro(String name, boolean is_default, Float dt) {
        super(name, is_default);
        this.dt = dt==null ? -1 : dt;
    }

    //////////////////////////////////////////////////
    // load
    //////////////////////////////////////////////////

    @Override
    public void validate(OTMErrorLog errorLog) {
        System.err.println("MICRO validate");
    }

    @Override
    public void reset(Link link) {
        System.err.println("MICRO reset");
    }

    @Override
    public void build() {
        System.err.println("MICRO build");
    }

    //////////////////////////////////////////////////
    // factory
    //////////////////////////////////////////////////

    @Override
    public AbstractVehicle create_vehicle(Long comm_id,Set<InterfaceVehicleListener> event_listeners) {
        return new models.micro.Vehicle(comm_id,event_listeners);
    }

    @Override
    public AbstractOutput create_output_object(Scenario scenario, String prefix, String output_folder, OutputRequest jaxb_or)  throws OTMException {
        AbstractOutput output = null;
        switch (jaxb_or.getQuantity()) {
            case "trajectories":
                Float outDt = jaxb_or.getDt();
                output = new OutputTrajectories(scenario, this,prefix, output_folder, outDt);
                break;
            default:
                throw new OTMException("Bad output identifier : " + jaxb_or.getQuantity());
        }
        return output;
    }

    // SIMILAR AS PQ
    @Override
    public AbstractLaneGroup create_lane_group(Link link, Side side, FlowDirection flowdir, Float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs) {
        return new models.micro.LaneGroup(link,side,flowdir,length,num_lanes,start_lane,out_rcs);
    }

    @Override
    public AbstractLinkInfo get_link_info(Link link) {
        System.err.println("MICRO get_link_info");
        return null;
    }

    @Override
    public AbstractVehicle translate_vehicle(AbstractVehicle that){
        if(that instanceof models.micro.Vehicle)
            return that;
        else
            return new models.micro.Vehicle(that);
    }

    //////////////////////////////////////////////////
    // run
    //////////////////////////////////////////////////

    @Override
    public void register_with_dispatcher(Scenario scenario, Dispatcher dispatcher, float start_time) {
        dispatcher.register_event(new EventPoke(dispatcher, 6,start_time + dt, this));
    }

    @Override
    public void poke(Dispatcher dispatcher, float timestamp) throws OTMException {
        update_state(timestamp);
        dispatcher.register_event(new EventPoke(dispatcher, 6,timestamp + dt, this));
    }

    private void update_state(float timestamp) throws OTMException{

        // apply Newell's update formula to all vehicles
        for(Link link : links) {
            for (AbstractLaneGroup alg : link.lanegroups_flwdn.values()) {
                models.micro.LaneGroup lg = (models.micro.LaneGroup) alg;
                for( models.micro.Vehicle vehicle : lg.vehicles ) {
                    double dx = Math.min(lg.dv, vehicle.headway - lg.dw);
                    dx = Math.min( dx , vehicle.headway * lg.dc);
                    dx = Math.max( dx , 0d );
                    vehicle.new_pos = vehicle.pos + dx;
                }
            }
        }

        // move vehicles to new link and update their headway
        for(Link link : links) {
            for (AbstractLaneGroup alg : link.lanegroups_flwdn.values()) {
                models.micro.LaneGroup lg = (models.micro.LaneGroup) alg;
                Iterator<Vehicle> it = lg.vehicles.iterator();
                while (it.hasNext()) {
                    Vehicle vehicle = it.next();
                    // possibly release the vehicle from this lanegroup
                    if (vehicle.new_pos > lg.length) {
                        vehicle.new_pos -= lg.length;
                        lg.release_vehicle(timestamp, it, vehicle);
                    }
                }
            }
        }

        // update position
        for(Link link : links) {
            for (AbstractLaneGroup alg : link.lanegroups_flwdn.values()) {
                models.micro.LaneGroup lg = (models.micro.LaneGroup) alg;
                Iterator<Vehicle> it = lg.vehicles.iterator();
                while (it.hasNext()) {
                    Vehicle vehicle = it.next();
                    vehicle.pos = vehicle.new_pos;
                }
            }
        }

        // update headway
        for(Link link : links) {
            for (AbstractLaneGroup alg : link.lanegroups_flwdn.values()) {
                models.micro.LaneGroup lg = (models.micro.LaneGroup) alg;
                Iterator<Vehicle> it = lg.vehicles.iterator();
                while (it.hasNext()) {
                    Vehicle vehicle = it.next();
                    if(vehicle.leader==null)
                        vehicle.headway = Double.POSITIVE_INFINITY;
                    else{
                        if(vehicle.leader.get_lanegroup()==vehicle.get_lanegroup())
                            vehicle.headway = vehicle.leader.pos - vehicle.pos;
                        else
                            vehicle.headway = vehicle.leader.pos - vehicle.pos + vehicle.get_lanegroup().length;
                    }
                }
            }
        }
    }



}
