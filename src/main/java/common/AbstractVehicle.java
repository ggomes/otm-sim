package common;

import commodity.Path;
import keys.State;
import output.InterfaceVehicleListener;
import utils.OTMUtils;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractVehicle {

    private long id;
    private Long comm_id;
    private State state;
    public AbstractLaneGroup lg;
    public Path path;

    // dispatch listeners
    private Set<InterfaceVehicleListener> event_listeners;

    public AbstractVehicle(){}

    public AbstractVehicle(AbstractVehicle that){
        this.id = that.getId();
        this.state = that.state;
        this.comm_id = that.comm_id;
        this.event_listeners = that.event_listeners;
    }

    public AbstractVehicle(Long comm_id,Set<InterfaceVehicleListener> event_listeners){
        this.id = OTMUtils.get_vehicle_id();
        this.comm_id = comm_id;
        this.event_listeners = new HashSet<>();
        if(event_listeners!=null)
            this.event_listeners.addAll(event_listeners);
        this.lg = null;
    }

    public void set_next_link_id(Long nextlink_id){
        if(state !=null && state.isPath)
            return;
        state = new State(comm_id,nextlink_id,false);
    }

    ////////////////////////////////////////////////
    // getters
    ////////////////////////////////////////////////

    public long getId(){
        return id;
    }

    public long get_commodity_id(){
        return comm_id;
    }

    public AbstractLaneGroup get_lanegroup(){
        return lg;
    }

    ////////////////////////////////////////////
    // event listeners
    ////////////////////////////////////////////

    public void add_event_listeners(Set<InterfaceVehicleListener> x){
        this.event_listeners.addAll(x);
    }

    public void remove_event_listeners(Set<InterfaceVehicleListener> x){
        this.event_listeners.removeAll(x);
    }

    public Set<InterfaceVehicleListener> get_event_listeners(){
        return event_listeners;
    }

    ////////////////////////////////////////////
    // state
    ////////////////////////////////////////////

    public void set_state(State state){
        assert(state.commodity_id==this.comm_id);
        this.state = state;
    }

    public State get_state(){
        return state;
    }

    public Long get_next_link_id(){
        if(lg.link.is_sink)
            return null;
        return state.isPath ? path.get_link_following(lg.link).getId() : state.pathOrlink_id;
    }

    // NOTE: We do not update the next link id when it is null. This happens in
    // sinks. This means that the state in a sink needs to be interpreted
    // differently, which must be accounted for everywhere.
//    public void set_next_link_id(Long next_link_id){
//        if(!key.isPath && next_link_id!=null)
//            key = new KeyCommPathOrLink(key.commodity_id,next_link_id,false);
//    }

    ///////////////////////////////////////
    // toString
    ///////////////////////////////////////

    @Override
    public String toString() {
        String str = "";
        str += "id " + id + "\n";
        str += "commodity_id " + comm_id + "\n";
        str += "in lanegroup " + (lg ==null?"none": lg.id) + "\n";
        return str;
    }

}
