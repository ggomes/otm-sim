/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package keys;

public class KeyCommodityDemandTypeId {

    public final long commodity_id;
    public final long link_or_subnetwork_id;
    public final DemandType demandType;

    public KeyCommodityDemandTypeId(long commodity_id, long link_or_subnetwork_id,DemandType demandType) {
        this.commodity_id = commodity_id;
        this.link_or_subnetwork_id = link_or_subnetwork_id;
        this.demandType = demandType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KeyCommodityDemandTypeId that = (KeyCommodityDemandTypeId) o;

        if (commodity_id != that.commodity_id) return false;
        if (link_or_subnetwork_id != that.link_or_subnetwork_id) return false;
        return demandType == that.demandType;
    }

    @Override
    public int hashCode() {
        int result = (int) (commodity_id ^ (commodity_id >>> 32));
        result = 31 * result + (int) (link_or_subnetwork_id ^ (link_or_subnetwork_id >>> 32));
        result = 31 * result + demandType.hashCode();
        return result;
    }
}
