package commodity;

import common.*;
import error.OTMErrorLog;
import error.OTMException;
import utils.OTMUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class Subnetwork implements InterfaceScenarioElement {

    protected final Long id;
    protected String name;
    protected Set<Link> links;
    protected Set<Commodity> used_by_comm;
    protected boolean is_path;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public Subnetwork(Long id,String name,Set<Long> link_ids,Set<Long> comm_ids,Scenario scenario) throws OTMException {

        if(!scenario.network.links.keySet().containsAll(link_ids))
            throw new OTMException("Bad link id in subnetwork constructor");

        if(!scenario.commodities.keySet().containsAll(comm_ids))
            throw new OTMException("Bad commodity id in subnetwork constructor");

        this.id  = id;
        this.name = name;
        this.links = link_ids.stream().map(i->scenario.network.links.get(i)).collect(Collectors.toSet());
        this.used_by_comm = comm_ids.stream().map(i->scenario.commodities.get(i)).collect(Collectors.toSet());
        this.is_path = check_is_path();
    }

    public Subnetwork(Subnetwork that){
        this.id = that.getId();
        this.name = that.getName();
        this.links = new HashSet<>();
        this.links.addAll(that.links);
        this.used_by_comm = new HashSet<>();
        this.used_by_comm.addAll(that.used_by_comm);
        this.is_path = that.is_path;
    }

    public Subnetwork(jaxb.Subnetwork js,Network network) throws OTMException{
        this.id = js.getId();
        this.name = js.getName();
        this.links = new HashSet<>();
        this.add_links(OTMUtils.csv2longlist(js.getContent()).stream().map(i->network.links.get(i)).collect(Collectors.toSet()));
        this.used_by_comm = new HashSet<>();
    }

    public Subnetwork(Network network) {
        this.id = 0L;
        this.name = "whole network";
        this.links = new HashSet<>();
        this.used_by_comm = new HashSet<>();
        for(Link link : network.links.values())
            links.add(link);
        this.is_path = check_is_path();
    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    public final Long getId() {
        return id;
    }

    @Override
    public final ScenarioElementType getSEType() {
        return ScenarioElementType.subnetwork;
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
    }

    @Override
    public jaxb.Subnetwork to_jaxb(){
        jaxb.Subnetwork jsub = new jaxb.Subnetwork();
        jsub.setId(this.getId());
        jsub.setName(this.getName());
        jsub.setContent(OTMUtils.comma_format(get_link_ids()));
        return jsub;
    }

    ///////////////////////////////////////////////////
    // get  / set
    ///////////////////////////////////////////////////

    public String getName(){
        return this.name;
    }

    public List<Long> get_link_ids(){
        return this.links.stream().map(x->x.getId()).collect(toList());
    }

    public List<Long> get_commodity_ids(){
        return this.used_by_comm.stream().map(x->x.getId()).collect(toList());
    }

    public boolean isPath(){
        return is_path;
    }

    public boolean has_link_id(Long link_id){
        return links.stream().anyMatch(x->x.getId().equals(link_id));
    }

    // This is overridden by Path to return the ordered list
    public Collection<Link> get_links(){
        return links;
    }

    public void add_commodity(Commodity c){
        used_by_comm.add(c);
    }

    public void add_links(Collection<common.Link> links) throws OTMException {
        if(links.contains(null))
            throw new OTMException("Attempted to add null link");
        this.links.addAll(links);
        this.is_path = check_is_path();
    }

    public void remove_links(Collection<common.Link> rlinks) {
        if(!Collections.disjoint(this.links,rlinks)){
            links.removeAll(rlinks);
            this.is_path = check_is_path();
        }
    }

    ///////////////////////////////////////////////////
    // private
    ///////////////////////////////////////////////////

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

            Collection<Link> next_links = current.end_node.out_links;
            Set<Link> next_link = OTMUtils.intersect(next_links,this.links);
            if(next_link.size()>1)
                return false;
            current = next_link.iterator().next();
            unchecked.remove(current);

        }
    }

}
