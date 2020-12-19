package actuator;

import core.Network;
import core.Scenario;
import control.command.CommandNumber;
import control.command.InterfaceCommand;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Actuator;
import core.AbstractLaneGroup;

import java.util.Set;

/** This is an abstract class for actuators whose target is
 * a set of lane group capacities.
 * The command is a single rate for all lane groups in veh/sec
 */
public abstract class AbstractActuatorLanegroupCapacity extends AbstractActuator {

    protected Set<AbstractLaneGroup> lanegroups;
    public final int total_lanes;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public AbstractActuatorLanegroupCapacity(Scenario scenario, Actuator jact) throws OTMException {
        super(scenario, jact);
        this.lanegroups = read_lanegroups(scenario.network,jact);
        this.total_lanes = lanegroups==null || lanegroups.isEmpty() ? 0 : lanegroups.stream().mapToInt(x->x.num_lanes).sum();
    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void validate(OTMErrorLog errorLog) {

        if(lanegroups==null || lanegroups.isEmpty()){
            errorLog.addWarning(String.format("Actuator %d has no lanegroups.",id));
            return;
        }

        // check that the lanes provided cover all lane, ie that total lanes
        // equals the sum of lanes in the lanegroups
        int lg_lanes = lanegroups.stream().mapToInt(x->x.num_lanes).sum();
        if(lg_lanes!=total_lanes)
            errorLog.addError("A lane group actuator must exactly cover its lane groups");
    }

    ///////////////////////////////////////////////////
    // AbstractActuator
    ///////////////////////////////////////////////////

    @Override
    public void process_controller_command(InterfaceCommand command, float timestamp) throws OTMException {
        if(command==null)
            return;
        Float rate_vps = ((CommandNumber) command).value;
        if(rate_vps==null)
            return;
        for(AbstractLaneGroup lg : lanegroups)
            lg.set_actuator_capacity_vps(rate_vps * lg.num_lanes / total_lanes);
    }

}
