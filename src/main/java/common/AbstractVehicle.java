/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package common;

import keys.KeyCommPathOrLink;
import output.InterfaceVehicleListener;
import utils.OTMUtils;

import java.util.Set;

public abstract class AbstractVehicle {

    private long id;
    private KeyCommPathOrLink key;

    protected AbstractLaneGroup my_lanegroup;

    // dispatch listeners
    private Set<InterfaceVehicleListener> event_listeners;

    public AbstractVehicle(){}

    public AbstractVehicle(AbstractVehicle that){
        this.id = that.getId();
        this.key = that.key;
        this.event_listeners = that.event_listeners;
    }

    public AbstractVehicle(KeyCommPathOrLink key,Set<InterfaceVehicleListener> vehicle_event_listeners){
        this.id = OTMUtils.get_vehicle_id();
        this.key = key;
        this.event_listeners = vehicle_event_listeners;
        this.my_lanegroup = null;
    }

//    public AbstractVehicle(Commodity commodity, Path path){
//        this.id = OTMUtils.get_vehicle_id();
//        this.key = new KeyCommPathOrLink(commodity.getId(),path.getId(), true);
//        this.event_listeners = commodity.vehicle_event_listeners;
//        this.my_lanegroup = null;
//    }

    @Override
    public String toString() {
        String str = "";
        str += "id " + id + "\n";
        str += "commodity_id " + key.commodity_id + "\n";
        str += "in lanegroup " + (my_lanegroup==null?"none":my_lanegroup.id) + "\n";
        return str;
    }

    ////////////////////////////////////////////////
    // getters
    ////////////////////////////////////////////////

    public long getId(){
        return id;
    }

    public KeyCommPathOrLink get_key(){
        return key;
    }

    public long get_commodity_id(){
        return key.commodity_id;
    }


    public AbstractLaneGroup get_lanegroup(){
        return my_lanegroup;
    }

    public void add_event_listeners(Set<InterfaceVehicleListener> x){
        this.event_listeners.addAll(x);
    }

    public void remove_event_listeners(Set<InterfaceVehicleListener> x){
        this.event_listeners.removeAll(x);
    }

    public Set<InterfaceVehicleListener> get_event_listeners(){
        return event_listeners;
    }

    // NOTE: We do not update the next link id when it is null. This happens in
    // sinks. This means that the state in a sink needs to be interpreted
    // differently, which must be accounted for everywhere.
    public void set_next_link_id(Long next_link_id){
        if(!key.isPath && next_link_id!=null)
            key = new KeyCommPathOrLink(key.commodity_id,next_link_id,false);
    }

}
