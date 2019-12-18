package commodity;

import error.OTMErrorLog;
import error.OTMException;
import common.Link;
import common.Network;
import utils.OTMUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class Subnetwork {

    protected final Long id;
    protected String name;
    public boolean is_global;
    public Set<Link> links;
    public Set<Commodity> used_by_comm;
    public boolean is_path;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public Subnetwork(Subnetwork that){
        this.id = that.getId();
        this.name = that.getName();
        this.links = new HashSet<>();
        this.links.addAll(that.links);
        this.used_by_comm = new HashSet<>();
        this.used_by_comm.addAll(that.used_by_comm);
        this.is_global = that.is_global;
        this.is_path = that.is_path;
    }

    public Subnetwork(jaxb.Subnetwork js,Network network) throws OTMException{
        this.id = js.getId();
        this.name = js.getName();
        this.links = new HashSet<>();
        for(Long link_id : OTMUtils.csv2longlist(js.getContent()))
            this.add_link(network.links.get(link_id));
        this.used_by_comm = new HashSet<>();
        this.is_global = links.size()==network.links.values().size();
        this.is_path = check_is_path();
    }

    public Subnetwork(Network network) {
        this.id = 0L;
        this.name = "whole network";
        this.is_global = true;
        this.links = new HashSet<>();
        this.used_by_comm = new HashSet<>();
        for(Link link : network.links.values())
            links.add(link);
        this.is_path = check_is_path();
    }

    public void validate(OTMErrorLog errorLog){
//        if( id<= 0 && !is_global)
//            scenario.error_log.addError("id<=0 not allowed for subnetworks.");
    }

//    public void add_lanegroup(AbstractLaneGroup lg){
//        if(lanegroups==null)
//            lanegroups = new HashSet<>();
//        lanegroups.add(lg);
//    }
//
//    public void add_lanegroups(Collection<AbstractLaneGroup> lgs){
//        if(lanegroups==null)
//            lanegroups = new HashSet<>();
//        lanegroups.addAll(lgs);
//    }

    public void add_commodity(Commodity c){
        used_by_comm.add(c);
    }

    public void add_link(common.Link link) throws OTMException {
        if(link==null)
            throw new OTMException("Attempted to add null link");
        links.add(link);
    }

    ///////////////////////////////////////////////////
    // get  / set
    ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getName(){
        return this.name;
    }

    public List<Long> get_link_ids(){
        return this.links.stream().map(x->x.getId()).collect(toList());
    }

    public List<Long> get_commodity_ids(){
        return this.used_by_comm.stream().map(x->x.getId()).collect(toList());
    }

    public boolean isGlobal(){
        return is_global;
    }

    public boolean has_link_id(Long link_id){
        return links.stream().anyMatch(x->x.getId().equals(link_id));
    }

    // This is overridden by Path to return the ordered list
    public Collection<Link> get_links_collection(){
        return links;
    }

    ///////////////////////////////////////////////////
    // private
    ///////////////////////////////////////////////////

//    private boolean check_is_path(){
//
//        // get all sources in the subnetwork
//        List<Link> sources = this.links.stream()
//                .filter(x->x.is_source)
//                .collect(toList());
//
//        if(sources.size()!=1)
//            return false;
//
//        // construct path
//        Link current = sources.get(0);
//        int num_checked = 1;
//        while(true){
//            Collection<Link> next_links = current.end_node.out_links.values();
//            Set<Link> next_link = OTMUtils.intersect(next_links,this.links);
//            if(next_link.size()!=1)
//                return false;
//            num_checked++;
//            if(num_checked>=this.links.size())
//                return true;
//            if(current.is_sink)
//                return false;
//        }
//    }

    private boolean check_is_path(){

        // check that there is exactly one source in links
        List<Link> sources = this.links.stream()
                .filter(x->x.is_source)
                .collect(toList());

        if(sources.size()!=1)
            return false;

        // construct path
        Link current = sources.get(0);
        Set<Link> unchecked = new HashSet<>();
        unchecked.addAll(links);
        unchecked.remove(current);

        while(true){

            if(unchecked.isEmpty())
                return true;

            Collection<Link> next_links = current.end_node.out_links.values();
            Set<Link> next_link = OTMUtils.intersect(next_links,this.links);
            if(next_link.size()<0)
                return false;
            current = next_link.iterator().next();
            unchecked.remove(current);

        }
    }

    public jaxb.Subnetwork to_jaxb(){
        jaxb.Subnetwork jsub = new jaxb.Subnetwork();
        jsub.setId(this.getId());
        jsub.setName(this.getName());
        jsub.setContent(OTMUtils.comma_format(get_link_ids()));
        return jsub;
    }
}
