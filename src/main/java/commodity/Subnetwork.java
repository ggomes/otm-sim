package commodity;

import core.*;
import error.OTMErrorLog;
import error.OTMException;
import utils.OTMUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class Subnetwork implements InterfaceScenarioElement {

    protected final Long id;
    protected String name;
    protected Set<Long> link_ids;
    protected Set<Commodity> used_by_comm;

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
        this.link_ids = link_ids;
        this.used_by_comm = comm_ids.stream().map(i->scenario.commodities.get(i)).collect(Collectors.toSet());
    }

    public Subnetwork(Subnetwork that){
        this.id = that.getId();
        this.name = that.getName();
        this.link_ids = new HashSet<>();
        this.link_ids.addAll(that.link_ids);
        this.used_by_comm = new HashSet<>();
        this.used_by_comm.addAll(that.used_by_comm);
    }

    public Subnetwork(jaxb.Subnetwork js) throws OTMException{
        this.id = js.getId();
        this.name = js.getName();
        this.link_ids = new HashSet<>();
        this.add_links(OTMUtils.csv2longlist(js.getContent()));
        this.used_by_comm = new HashSet<>();
    }

//    public Subnetwork(Network network) {
//        this.id = 0L;
//        this.name = "whole network";
//        this.link_ids = network.links.keySet();
//        this.used_by_comm = new HashSet<>();
//    }

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

    public void validate_pre_init(OTMErrorLog errorLog) {
    }

    public void validate_post_init(OTMErrorLog errorLog){

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

    public Collection<Long> get_link_ids(){
        return link_ids;
    }

    public List<Long> get_commodity_ids(){
        return this.used_by_comm.stream().map(x->x.getId()).collect(toList());
    }

    public boolean has_link_id(Long link_id){
        return link_ids.contains(link_id);
    }

    public void add_commodity(Commodity c){
        used_by_comm.add(c);
    }

    public void add_links(Collection<Long> new_link_ids) throws OTMException {
        if(new_link_ids.contains(null))
            throw new OTMException("Attempted to add null link");
        this.link_ids.addAll(new_link_ids);
    }

    public void remove_links(Collection<Long> rlink_ids) {
        link_ids.removeAll(rlink_ids);
    }

}
