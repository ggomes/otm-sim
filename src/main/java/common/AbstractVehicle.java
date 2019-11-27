package common;

import commodity.Path;
import keys.KeyCommPathOrLink;
import models.BaseLaneGroup;
import output.InterfaceVehicleListener;
import utils.OTMUtils;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractVehicle {

    private long id;
    private Long comm_id;
    private KeyCommPathOrLink key;
    public BaseLaneGroup lg;
    public Path path;

    // dispatch listeners
    private Set<InterfaceVehicleListener> event_listeners;

    public AbstractVehicle(){}

    public AbstractVehicle(AbstractVehicle that){
        this.id = that.getId();
        this.key = that.key;
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
        if(key.isPath)
            return;
        key = new KeyCommPathOrLink(comm_id,nextlink_id,false);
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

    public BaseLaneGroup get_lanegroup(){
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
    // key
    ////////////////////////////////////////////

    public void set_key(KeyCommPathOrLink key){
        assert(key.commodity_id==this.comm_id);
        this.key = key;
    }

    public KeyCommPathOrLink get_key(){
        return key;
    }

    public Long get_next_link_id(){
        if(lg.link.is_sink)
            return null;
        return key.isPath ? path.get_link_following(lg.link).getId() : key.pathOrlink_id;
    }

    // NOTE: We do not update the next link id when it is null. This happens in
    // sinks. This means that the state in a sink needs to be interpreted
    // differently, which must be accounted for everywhere.
//    public void set_next_link_id(Long next_link_id){
//        if(!key.isPath && next_link_id!=null)
//            key = new KeyCommPathOrLink(key.commodity_id,next_link_id,false);
//    }


    @Override
    public String toString() {
        String str = "";
        str += "id " + id + "\n";
        str += "commodity_id " + comm_id + "\n";
        str += "in lanegroup " + (lg ==null?"none": lg.id) + "\n";
        return str;
    }

}
