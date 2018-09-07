/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package keys;

public class KeyCommodityNodeLink {
    public final long commodity_id;
    public final long node_id;
    public final long linkin_id;
    public KeyCommodityNodeLink(long commodity_id, long node_id, long linkin_id) {
        this.commodity_id = commodity_id;
        this.node_id = node_id;
        this.linkin_id = linkin_id;
    }
}
