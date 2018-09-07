/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package api.info;

import common.Node;

public class NodeInfo {

    /** Integer id of the node. */
    public long id;

    public NodeInfo(Node x){
        this.id = x.getId();
    }

    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "NodeInfo{" +
                "id=" + id +
                '}';
    }

}
