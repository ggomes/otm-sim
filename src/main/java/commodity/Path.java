package commodity;

import common.*;
import error.OTMErrorLog;
import error.OTMException;
import utils.OTMUtils;

import java.util.*;

import static java.util.stream.Collectors.toList;

public class Path extends Subnetwork {

    public ArrayList<Link> ordered_links;

    public Path(jaxb.Subnetwork js, Network network) throws OTMException {
        super(js, network);
        create_ordered_links();
    }

    public Path(Network network) {
        super(network);
        create_ordered_links();
    }

    public Path(Subnetwork subnet) {
        super(subnet);
        create_ordered_links();
    }

    private void create_ordered_links(){

        // generate ordered links ..........
        // get all sources in the subnetwork
        // guaranteed to be single source because it is a path
        List<Link> sources = this.links.stream()
                .filter(x->x.is_source)
                .collect(toList());
        Link current = sources.get(0);

        Set<Link> unchecked = new HashSet<>();
        unchecked.addAll(links);
        unchecked.remove(current);

        ordered_links = new ArrayList<>();
        ordered_links.add(current);

        while(!unchecked.isEmpty()){

            Collection<Link> next_links = current.end_node.out_links.values();
            Set<Link> next_link = OTMUtils.intersect(next_links,links);

            current = next_link.iterator().next();
            ordered_links.add(current);
            unchecked.remove(current);
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

//    // Returns null if it is a sink or a x-to-one case with no road connections defined
//    public RoadConnection get_roadconn_following(AbstractLaneGroup lg){
//        if(lg==null)
//            return null;
//        Link next_link = get_link_following(lg.link);
//        if(next_link!=null)
//            return lg.get_roadconnection_for_outlink(next_link.getId());
//        else
//            return null;
//    }

    public boolean has(Link link){
        return this.ordered_links.contains(link);
    }

    @Override
    public Collection<Link> get_links_collection() {
        return ordered_links;
    }

}
