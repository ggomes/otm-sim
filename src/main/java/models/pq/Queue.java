/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.pq;

import common.Link;
import error.OTMErrorLog;
import error.OTMException;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import static java.util.stream.Collectors.toSet;

public class Queue {

    public enum Type {transit,waiting}

    public final String id;
    public final Queue.Type type;
    public final LaneGroup lanegroup;
    private List<Vehicle> vehicles;
    private PriorityQueue<LaneChangeRequest> lane_change_requests;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public Queue(LaneGroup lanegroup, Queue.Type type) {
        this.type = type;
        this.lanegroup = lanegroup;
        this.vehicles = new ArrayList<>();
        this.lane_change_requests = new PriorityQueue<>(LaneChangeRequest::compareTimestamp);
        switch(type){
            case transit:
                id = "t" + lanegroup.id;
                break;
            case waiting:
                id = "w" + lanegroup.id;
                break;
            default:
                id = "";
                break;
        }
    }

    public void validate(OTMErrorLog errorLog) {
    }

    public void initialize() throws OTMException {
        vehicles = new ArrayList<>();
    }

    ///////////////////////////////////////////////////
    // update
    ///////////////////////////////////////////////////

    public Vehicle peek_vehicle() {
        return vehicles.isEmpty() ? null : vehicles.get(0);
    }

    public void remove_given_vehicle(float timestamp, Vehicle v) throws OTMException {
        this.vehicles.remove(v);

        // process any lane change requests
        Link link = lanegroup.link;
        ((Model_PQ)link.model).process_lane_change_request(link,timestamp,lane_change_requests.poll());

    }

    public void add_vehicle(Vehicle v) {
        this.vehicles.add(v);
    }

    public long num_vehicles_for_commodity(Long c) {
        return c==null ? vehicles.size() : vehicles.stream().filter(x -> x.get_commodity_id()==c).count();
    }

    public int num_vehicles(){
        return vehicles.size();
    }

    public void submit_lane_change_request(LaneChangeRequest r){
        this.lane_change_requests.add(r);
    }

    protected void remove_lane_change_requests_for_vehicle(Vehicle vehicle){
        lane_change_requests.removeAll(
                lane_change_requests.stream()
                        .filter(x->x.requester==vehicle)
                        .collect(toSet()) );
    }

}
