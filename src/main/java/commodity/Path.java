package commodity;

import core.*;
import error.OTMErrorLog;
import error.OTMException;
import utils.OTMUtils;

import java.util.*;
import java.util.stream.Collectors;

public class Path extends Subnetwork {

    public ArrayList<Link> ordered_links;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public Path(jaxb.Subnetwork js) throws OTMException {
        super(js);
    }

    public boolean populate_ordered_links(Network network){

        Set<Link> links = this.link_ids.stream()
                .map(x->network.links.get(x))
                .collect(Collectors.toSet());

        // check that there is exactly one source in links
        Set<Link> sources = links.stream()
                .filter(x->x.is_source())
                .collect(Collectors.toSet());

        if(sources.size()!=1)
            return false;

        Link current = sources.iterator().next();
        Set<Link> unchecked = new HashSet<>();
        unchecked.addAll(links);
        unchecked.remove(current);

        ordered_links = new ArrayList<>();
        ordered_links.add(current);

        while(!unchecked.isEmpty()){
            Set<Link> next_link = OTMUtils.intersect(current.get_end_node().get_out_links(), links);
            if(next_link.size()>1)
                return false;
            current = next_link.iterator().next();
            ordered_links.add(current);
            unchecked.remove(current);
        }

        return true;
    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void validate_pre_init(OTMErrorLog errorLog) {

        // must have at least two links
        if(ordered_links.size()<2)
            errorLog.addError("ordered_links.size()<2");

        // first link must be a source
        if(!ordered_links.get(0).is_source())
            errorLog.addError("first link in path is not a source");
    }

    ///////////////////////////////////////////////////
    // get
    ///////////////////////////////////////////////////

    public Link get_origin(){
        return ordered_links.get(0);
    }

    public Long get_origin_node_id(){
        return get_origin().get_start_node().getId();
    }

    public Link get_destination(){
        return ordered_links.get(ordered_links.size()-1);
    }

    public Long get_destination_node_id(){
        return get_destination().get_end_node().getId();
    }

    public Link get_link_following(Link link){
        if(ordered_links==null)
            return null;
        int ind = ordered_links.indexOf(link)+1;
        return ind>ordered_links.size()-1 ? null : ordered_links.get(ind);
    }

    public boolean has(Link link){
        return this.ordered_links.contains(link);
    }

    public List<Link> get_ordered_links() {
        return ordered_links;
    }

}
