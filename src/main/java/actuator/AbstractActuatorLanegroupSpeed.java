package actuator;

import common.AbstractLaneGroup;
import common.Scenario;
import control.CommandNumber;
import control.InterfaceCommand;
import error.OTMException;
import jaxb.Actuator;

import java.util.Set;

/** This is an abstract class for actuators whose target is
 * a set of lane group capacities.
 * The command is a single rate for all lane groups in veh/sec
 */
public abstract class AbstractActuatorLanegroupSpeed extends AbstractActuator {

    protected Set<AbstractLaneGroup> lanegroups;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public AbstractActuatorLanegroupSpeed(Scenario scenario, Actuator jact) throws OTMException {
        super(scenario, jact);
        this.lanegroups = read_lanegroups(scenario,jact);
    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void initialize(Scenario scenario) throws OTMException {

    }

    ///////////////////////////////////////////////////
    // AbstractActuator
    ///////////////////////////////////////////////////

    @Override
    public void process_controller_command(InterfaceCommand command, float timestamp) throws OTMException {
        if(command==null)
            return;
        double value = ((CommandNumber)command).value;
        for(AbstractLaneGroup lg : lanegroups)
            lg.set_actuator_speed_mps(value);
    }

}
