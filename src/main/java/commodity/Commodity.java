package commodity;

import dispatch.Dispatcher;
import keys.DemandType;
import models.AbstractLaneGroup;
import common.Link;
import error.OTMErrorLog;
import error.OTMException;
import output.InterfaceVehicleListener;
import common.InterfaceScenarioElement;
import common.Scenario;
import common.ScenarioElementType;
import utils.OTMUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class Commodity implements InterfaceScenarioElement {

    protected final Long id;
    public final String name;
    public final Set<Subnetwork> subnetworks;
    public  Set<Link> all_links;
    public boolean pathfull;
    public float pvequiv;

    // this is a dispatch output writer for vehicles of this commodity
    public Set<InterfaceVehicleListener> vehicle_event_listeners;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public Commodity(long id, String name, List<Long> subnet_ids, Scenario scenario){
        this.id = id;
        this.name = name;
        this.subnetworks = new HashSet<>();
        this.pvequiv = 1f;
        this.all_links = new HashSet<>();
        if(subnet_ids!=null)
            for(Long subnet_id : subnet_ids){
                Subnetwork subnet = scenario.subnetworks.get(subnet_id);
                if(subnet!=null)
                    this.subnetworks.add(subnet);
//                all_lanegroups.addAll(subnet.lanegroups);
                all_links.addAll(subnet.links);
            }
        this.vehicle_event_listeners = new HashSet<>();
        this.pathfull = false;
    }

    public Commodity(jaxb.Commodity jaxb_comm, List<Long> subnet_ids, Scenario scenario) throws OTMException {
        this.id = jaxb_comm.getId();
        this.name = jaxb_comm.getName();
        this.pathfull = jaxb_comm.isPathfull();
        this.subnetworks = new HashSet<>();
        this.pvequiv = jaxb_comm.getPvequiv();
        this.all_links = new HashSet<>();
        if(subnet_ids!=null)
            for(Long subnet_id : subnet_ids){
                Subnetwork subnet = scenario.subnetworks.get(subnet_id);
                if(subnet!=null) {
                    this.subnetworks.add(subnet);
                    all_links.addAll(subnet.links);
                }
            }
        this.vehicle_event_listeners = new HashSet<>();
    }

    ///////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public ScenarioElementType getType() {
        return ScenarioElementType.commodity;
    }

    @Override
    public void validate(OTMErrorLog errorLog) {

    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        for(Subnetwork subnetwork : subnetworks)
            for(Link link : subnetwork.links)
                register_commodity(link,this,subnetwork);
    }

    @Override
    public void register_with_dispatcher(Dispatcher dispatcher) {

    }

    @Override
    public jaxb.Commodity to_jaxb(){
        jaxb.Commodity jcomm = new jaxb.Commodity();
        jcomm.setId(getId());
        jcomm.setName(name);
        jcomm.setPathfull(pathfull);
        jcomm.setPvequiv(pvequiv);

        List<Long> subnets = subnetworks.stream().map(x->x.getId()).collect(Collectors.toList());

        // exclude subnetwork 0
        subnets.remove(0l);

        if(!subnets.isEmpty()) {
            String str = OTMUtils.comma_format(subnetworks.stream().map(x -> x.getId()).collect(Collectors.toList()));
            jcomm.setSubnetworks(str);
        }
        return jcomm;
    }

    ///////////////////////////////////////////////////
    // get  / set
    ///////////////////////////////////////////////////

    public DemandType get_demand_type(){
        return pathfull ? DemandType.pathfull : DemandType.pathless;
    }

    public boolean travels_on_link(Link link){
        return all_links.contains(link);
    }

    public void add_vehicle_event_listener(InterfaceVehicleListener ev) {
        vehicle_event_listeners.add(ev);
    }

    public List<Long> get_subnetwork_ids(){
        return this.subnetworks.stream().map(x->x.id).collect(toList());
    }

    public Set<Subnetwork> get_subnetworks_for_link(Link link){
        Set<Subnetwork> x = new HashSet<>();

        // all links are members of the single subnetwork for pathless commodities
        if(!pathfull) {
            x.add(subnetworks.iterator().next());
            return x;
        }

        // otherwise check all subnetworks for this link
        for(Subnetwork subnetwork : subnetworks)
            if(subnetwork.links.contains(link))
                x.add(subnetwork);
        return x;
    }

    public Set<Subnetwork> get_subnetworks_for_lanegroup(AbstractLaneGroup lg){
        Link link = lg.link;
        Set<Long> next_link_ids = lg.get_dwn_links();
        Set<Subnetwork> x = new HashSet<>();
        for(Subnetwork subnetwork : subnetworks) {
            if (subnetwork.links.contains(link) && subnetwork.links.stream().anyMatch(z->next_link_ids.contains(z.getId())))
                x.add(subnetwork);
        }
        return x;
    }

    public String get_name(){
        return name;
    }

//    public Path get_path(){
//        return this.path;
//    }


    ////////////////////////////////////////////
    // private
    ///////////////////////////////////////////

    private static void register_commodity(Link link,Commodity comm, Subnetwork subnet) throws OTMException {

        if(comm.pathfull) {
            Link next_link = ((Path) subnet).get_link_following(link);
            Long next_link_id = next_link==null ? null : next_link.getId();
            for (AbstractLaneGroup lg : link.lanegroups_flwdn.values())
                lg.add_state(comm.getId(), subnet.getId(),next_link_id, true);
        }

        else {

            // for pathless/sink, next link id is same as this id
            if (link.is_sink) {
                for (AbstractLaneGroup lg : link.lanegroups_flwdn.values())
                    lg.add_state(comm.getId(), null,link.getId(), false);

            } else {

                // for pathless non-sink, add a state for each next link
                for( Long next_link_id : link.outlink2lanegroups.keySet()  ){
                    if (!subnet.has_link_id(next_link_id))
                        continue;
                    for (AbstractLaneGroup lg : link.lanegroups_flwdn.values())
                        lg.add_state(comm.getId(), null,next_link_id, false);
                }
            }
        }

    }

}
