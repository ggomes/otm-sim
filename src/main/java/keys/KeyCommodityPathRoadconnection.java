/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package keys;

import java.util.Objects;

public class KeyCommodityPathRoadconnection {

    public final Long commodity_id;
    public final Long subnetwork_id;   // null means that the commodity is pathless
    public final Long rc_id;            // road connection id

    public KeyCommodityPathRoadconnection(Long commodity_id, Long subnetwork_id, Long rc_id) {
        this.commodity_id = commodity_id;
        this.subnetwork_id = subnetwork_id;
        this.rc_id = rc_id;
    }

    public KeyCommodityPath getCommPath(){
        return new KeyCommodityPath(commodity_id,subnetwork_id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyCommodityPathRoadconnection that = (KeyCommodityPathRoadconnection) o;
        return Objects.equals(commodity_id, that.commodity_id) &&
                Objects.equals(subnetwork_id, that.subnetwork_id) &&
                Objects.equals(rc_id, that.rc_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commodity_id, subnetwork_id, rc_id);
    }

}
