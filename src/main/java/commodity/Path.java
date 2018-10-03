/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package commodity;

import common.*;
import error.OTMErrorLog;
import error.OTMException;
import runner.Scenario;
import utils.OTMUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class Path extends Subnetwork {

    public ArrayList<Link> ordered_links;

    public Path(jaxb.Subnetwork js, Network network) throws OTMException {
        super(js, network);

        // generate ordered links ..........
        // get all sources in the subnetwork
        // guaranteed to be single source because it is a path
        List<Link> sources = this.links.stream()
                .filter(x->x.is_source)
                .collect(toList());
        Link current = sources.get(0);
        ordered_links = new ArrayList<>();
        ordered_links.add(current);

        while(true){
            Collection<Link> next_links = current.get_next_links();
            Collection<Link> next_link = OTMUtils.intersect(next_links,links);

            if(next_link.size()>1) {
                System.err.println("Not able to construct this path");
                return;
            }

            current = next_link.iterator().next();
            ordered_links.add(current);
            if(ordered_links.size()>=links.size())
                break;
            if(current.is_sink)
                break;
        }
    }

    public void validate(OTMErrorLog errorLog) {

        // must have at least two links
        if(ordered_links.size()<2)
            errorLog.addError("ordered_links.size()<2");

        // first link must be a source
        if(!ordered_links.get(0).is_source)
            errorLog.addError("first link in path is not a source");
    }

    public Link get_origin(){
        return ordered_links.get(0);
    }

    public Long get_origin_node_id(){
        return get_origin().start_node.getId();
    }

    public Link get_destination(){
        return ordered_links.get(ordered_links.size()-1);
    }

    public Long get_destination_node_id(){
        return get_destination().end_node.getId();
    }

    public Link get_link_following(Link link){
        if(ordered_links==null)
            return null;
        int ind = ordered_links.indexOf(link)+1;
        return ind>ordered_links.size()-1 ? null : ordered_links.get(ind);
    }

    // Returns null if it is a sink or a x-to-one case with no road connections defined
    public RoadConnection get_roadconn_following(AbstractLaneGroupLongitudinal lg){
        if(lg==null)
            return null;
        Link next_link = get_link_following(lg.link);
        if(next_link!=null)
            return lg.get_roadconnection_for_outlink(next_link.getId());
        else
            return null;
    }

    public boolean has(Link link){
        return this.ordered_links.contains(link);
    }

    @Override
    public Collection<Link> get_links_collection() {
        return ordered_links;
    }

}
