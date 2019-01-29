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
        System.out.println("MICRO validate");
    }

    @Override
    public void reset(Link link) {
        System.out.println("MICRO reset");
    }

    @Override
    public void build() {
        System.out.println("MICRO build");
    }

    //////////////////////////////////////////////////
    // factory
    //////////////////////////////////////////////////

    @Override
    public AbstractVehicle create_vehicle(Long comm_id,Set<InterfaceVehicleListener> event_listeners) {
        return new models.micro.Vehicle(comm_id,event_listeners);
    }

    @Override
    public AbstractOutput create_output_object(Scenario scenario, String prefix, String output_folder, OutputRequest jaxb_or) throws OTMException {
        System.out.println("MICRO create_output_object");
        return null;
    }

    // SIMILAR AS PQ
    @Override
    public AbstractLaneGroup create_lane_group(Link link, Side side, FlowDirection flowdir, Float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs) {
        return new models.micro.LaneGroup(link,side,flowdir,length,num_lanes,start_lane,out_rcs);
    }

    @Override
    public AbstractLinkInfo get_link_info(Link link) {
        System.out.println("MICRO get_link_info");
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
        update_state();
        dispatcher.register_event(new EventPoke(dispatcher, 6,timestamp + dt, this));
    }

    private void update_state(){
        for(Link link : links) {
            for (AbstractLaneGroup lg : link.lanegroups_flwdn.values()) {
                List<models.micro.Vehicle> vhs = ((LaneGroup) lg).vehicles;

                // compute acceleration
                double [] acc = new double[vhs.size()];
                for(int i=0;i<vhs.size();i++) {
                    Vehicle v = vhs.get(i);
                    acc[i] = i == 0 ? leader_law(v) : follower_law(v,vhs.get(i-1));
                }

                // update position
                for(int i=0;i<vhs.size();i++){
                    Vehicle v = vhs.get(i);
                    v.speed += acc[i]*dt;
                    v.pos += v.speed*dt;
                }

            }
        }
    }

    private double leader_law(Vehicle v){
        return Double.NaN;
    }

    private double follower_law(Vehicle v,Vehicle lead){
        return Double.NaN;
    }

}
