/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package keys;

public class KeyCommodityPath {

    public final Long commodity_id;
    public final Long subnetwork_id;   // null means that the commodity is pathless

    public KeyCommodityPath(Long commodity_id,Long subnetwork_id) {
        this.commodity_id = commodity_id;
        this.subnetwork_id = subnetwork_id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KeyCommodityPath that = (KeyCommodityPath) o;

        if (commodity_id != that.commodity_id) return false;
        return subnetwork_id == that.subnetwork_id;
    }

    @Override
    public int hashCode() {
        int result = (int) (commodity_id ^ (commodity_id >>> 32));
        result = 31 * result + (int) (subnetwork_id ^ (subnetwork_id >>> 32));
        return result;
    }
}
