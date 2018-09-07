/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package tools.splitter;

import java.util.*;
import java.util.stream.Collectors;

public class SplitNode {

    long node_id;
    Map<String, Set<Long>> graph2links;  // map from graph name to graph2links

    public SplitNode(long node_id) {
        this.node_id = node_id;
        this.graph2links = new HashMap<>();
    }

    public void add_link(String name, Long link_id) {
        if (!graph2links.containsKey(name))
            graph2links.put(name, new HashSet<>());
        this.graph2links.get(name).add(link_id);
    }

    public void add_links(String name, Collection<Long> link_ids) {
        if (!graph2links.containsKey(name))
            graph2links.put(name, new HashSet<>());
        this.graph2links.get(name).addAll(link_ids);
    }

    public Set<Long> get_all_links() {
        return graph2links.values().stream().flatMap(x->x.stream()).collect(Collectors.toSet());
    }

    public Set<Long> get_links(String name) {
        return graph2links.containsKey(name) ? graph2links.get(name) : null;
    }

}
