/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package api.info;

import commodity.Commodity;

import java.util.List;
import java.util.stream.Collectors;

public class CommodityInfo {

    /** Integer id of the commodity. */
    public long id;

    /** Name of the commodity. */
    public String name;

    /** List of subnetwork ids for this commodity. */
    public List<Long> subnetwork_ids;

    /** True if the commodity is restricted to one or more paths. False otherwise. */
    public boolean pathfull;

    public CommodityInfo(Commodity x){
        this.id = x.getId();
        this.name = x.name;
        this.pathfull = x.pathfull;
        this.subnetwork_ids = x.subnetworks.stream().map(z->z.getId()).collect(Collectors.toList());
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Long> getSubnetwork_ids() {
        return subnetwork_ids;
    }

    public boolean isPathfull() {
        return pathfull;
    }

    @Override
    public String toString() {
        return "CommodityInfo{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", pathfull=" + pathfull +
                ", subnetwork_ids=" + subnetwork_ids +
                '}';
    }
}
