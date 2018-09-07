/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package api.info;

import commodity.Commodity;
import common.Link;

import java.util.*;

import static java.util.stream.Collectors.toList;

public class PerformanceInfo {

    /** XXX */
    public Commodity commodity;

    /** XXX */
    public List<Link> links = new ArrayList<>();

    /** XXX */
    public List<Double> vehicles = new ArrayList<>();

    /** XXX */
    public double total_vehicles;

    public PerformanceInfo(Commodity commodity, Collection<Link> clinks){
        this.commodity = commodity;
        this.links.addAll(clinks);
        total_vehicles = 0f;
        for(Link link : this.links){
            double v = link.get_veh_for_commodity(commodity==null?null:commodity.getId());
            vehicles.add(v);
            total_vehicles += v;
        }
    }

    public Commodity getCommodity() {
        return commodity;
    }

    public List<Long> getLinksIds() {
        return links.stream().map(x->x.getId()).collect(toList());
    }

    public List<Double> getVehicles() {
        return vehicles;
    }

    public double getTotal_vehicles() {
        return total_vehicles;
    }

    public double get_total_vehicles(){
        return total_vehicles;
    }
}
