package actuator;

import commodity.Commodity;
import common.AbstractLaneGroup;
import common.Link;
import common.Scenario;
import control.command.InterfaceCommand;
import error.OTMException;
import jaxb.Actuator;
import utils.OTMUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ActuatorLanegroupClosure extends AbstractActuator {

    public Commodity comm;
    public Set<AbstractLaneGroup> lanegroups;
    public boolean ison;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ActuatorLanegroupClosure(Scenario scenario, Actuator jaxb_actuator) throws OTMException {
        super(scenario, jaxb_actuator);
        this.comm = scenario.commodities.get(jaxb_actuator.getActuatorTarget().getId());
        this.lanegroups = new HashSet<>();
        if(jaxb_actuator.getLinklanes()!=null){
            for(jaxb.Linklane ll : jaxb_actuator.getLinklanes().getLinklane()){
                Link link = scenario.network.links.get(ll.getLink());
                if(link==null)
                    continue;
                Collection<AbstractLaneGroup> lgs;
                if(ll.getLanes()!=null && !ll.getLanes().isEmpty()){
                    int [] lanes = OTMUtils.read_lanes(ll.getLanes(),link.full_lanes);
                    lgs = link.get_unique_lanegroups_for_dn_lanes(lanes[0],lanes[1]);
                } else {
                    lgs = link.lanegroups_flwdn.values();
                }
                lanegroups.addAll(lgs);
            }
        }
        this.ison = true;
    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        System.out.println("Initialize ACTUATOR");
    }

    ///////////////////////////////////////////////////
    // AbstractActuator
    ///////////////////////////////////////////////////

    @Override
    public void process_controller_command(InterfaceCommand command, float timestamp) throws OTMException {
        System.out.println("process_controller_command ACTUATOR");

    }

}
