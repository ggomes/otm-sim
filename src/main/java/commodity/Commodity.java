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

    public Commodity(jaxb.Commodity jaxb_comm, List<Long> subnet_ids, Map<Long, Subnetwork> subnetworks) throws OTMException {
        this.id = jaxb_comm.getId();
        this.name = jaxb_comm.getName();
        this.pathfull = jaxb_comm.isPathfull();
        this.subnetworks = pathfull ? subnet_ids.stream().map(id->subnetworks.get(id)).collect(Collectors.toSet()) : null;
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

    public void validate_pre_init(OTMErrorLog errorLog) {
        if(pathfull && subnetworks.isEmpty())
            errorLog.addError("Pathfull commodity lacks routes");
        if(subnetworks!=null && subnetworks.contains(null))
            errorLog.addError("Bad subnetwork id in commodity");
    }

    @Override
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


}
