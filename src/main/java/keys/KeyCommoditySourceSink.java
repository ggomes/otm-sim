/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package keys;

public class KeyCommoditySourceSink {

    public final long commodity_id;
    public final long source_id;
    public final long sink_id;

    public KeyCommoditySourceSink(long commodity_id, long source_id, long sink_id) {
        this.commodity_id = commodity_id;
        this.source_id = source_id;
        this.sink_id = sink_id;
    }
}
