package commodity;

import core.*;
import error.OTMErrorLog;
import error.OTMException;
import output.InterfaceVehicleListener;
import utils.OTMUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class Commodity implements InterfaceScenarioElement {

    protected final Long id;
    public final String name;
    public final Set<Subnetwork> subnetworks;
    public boolean pathfull;
    public float pvequiv;

    // this is a dispatch output writer for vehicles of this commodity
    public Set<InterfaceVehicleListener> vehicle_event_listeners;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public Commodity(jaxb.Commodity jaxb_comm, List<Long> subnet_ids, Scenario scenario) throws OTMException {
        this.id = jaxb_comm.getId();
        this.name = jaxb_comm.getName();
        this.pathfull = jaxb_comm.isPathfull();
        this.subnetworks = pathfull ? subnet_ids.stream().map(id->scenario.subnetworks.get(id)).collect(Collectors.toSet()) : null;
        this.pvequiv = jaxb_comm.getPvequiv();
        this.vehicle_event_listeners = new HashSet<>();
    }

    ///////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////

    @Override
    public final Long getId() {
        return id;
    }

    @Override
    public final ScenarioElementType getSEType() {
        return ScenarioElementType.commodity;
    }

    @Override
    public void validate(OTMErrorLog errorLog) {

    }

//    @Override
    public jaxb.Commodity to_jaxb(){
        jaxb.Commodity jcomm = new jaxb.Commodity();
        jcomm.setId(getId());
        jcomm.setName(name);
        jcomm.setPathfull(pathfull);
        jcomm.setPvequiv(pvequiv);

        if(pathfull) {
            List<Long> subnets = subnetworks.stream().map(x -> x.getId()).collect(Collectors.toList());

            if(!subnets.isEmpty()) {
                String str = OTMUtils.comma_format(subnetworks.stream().map(x -> x.getId()).collect(Collectors.toList()));
                jcomm.setSubnetworks(str);
            }

            // exclude subnetwork 0
            subnets.remove(0l);
        }

        return jcomm;
    }

    ///////////////////////////////////////////////////
    // get  / set
    ///////////////////////////////////////////////////

    public DemandType get_demand_type(){
        return pathfull ? DemandType.pathfull : DemandType.pathless;
    }

    public void add_vehicle_event_listener(InterfaceVehicleListener ev) {
        vehicle_event_listeners.add(ev);
    }


    public List<Long> get_subnetwork_ids(){
        return pathfull ? this.subnetworks.stream().map(x->x.id).collect(toList()) : new ArrayList<>();
    }

    public String get_name(){
        return name;
    }

    ////////////////////////////////////////////
    // private
    ///////////////////////////////////////////

    public static void register_commodity(Link link, Commodity comm, Subnetwork subnet) throws OTMException {

        if(comm.pathfull) {
            Link next_link = ((Path) subnet).get_link_following(link);
            Long next_link_id = next_link==null ? null : next_link.getId();
            for (AbstractLaneGroup lg : link.lgs)
                lg.add_state(comm.getId(), subnet.getId(),next_link_id, true);
        }

        else {

            // for pathless/sink, next link id is same as this id
            if (link.is_sink) {
                for (AbstractLaneGroup lg : link.lgs)
                    lg.add_state(comm.getId(), null,link.getId(), false);

            } else {

                // for pathless non-sink, add a state for each next link
                for( Long next_link_id : link.outlink2lanegroups.keySet()  ){
                    for (AbstractLaneGroup lg : link.lgs)
                        lg.add_state(comm.getId(), null,next_link_id, false);
                }
            }
        }

    }

}
