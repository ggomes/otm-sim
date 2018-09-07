/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package api.info;

public class ODPair {

    public final Long origin_node_id;
    public final Long destination_node_id;
    public final Long commodity_id;

    public ODPair(Long origin_node_id, Long destination_node_id, Long commodity_id) {
        this.origin_node_id = origin_node_id;
        this.destination_node_id = destination_node_id;
        this.commodity_id = commodity_id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ODPair odPair = (ODPair) o;

        if (origin_node_id != null ? !origin_node_id.equals(odPair.origin_node_id) : odPair.origin_node_id != null) return false;
        if (destination_node_id != null ? !destination_node_id.equals(odPair.destination_node_id) : odPair.destination_node_id != null)
            return false;
        return commodity_id != null ? commodity_id.equals(odPair.commodity_id) : odPair.commodity_id == null;
    }

    @Override
    public int hashCode() {
        int result = origin_node_id != null ? origin_node_id.hashCode() : 0;
        result = 31 * result + (destination_node_id != null ? destination_node_id.hashCode() : 0);
        result = 31 * result + (commodity_id != null ? commodity_id.hashCode() : 0);
        return result;
    }
}
