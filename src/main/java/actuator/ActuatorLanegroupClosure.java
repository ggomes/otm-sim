package actuator;

import commodity.Commodity;
import common.AbstractLaneGroup;
import common.Scenario;
import control.command.InterfaceCommand;
import error.OTMException;
import jaxb.Actuator;

import java.util.HashSet;
import java.util.Set;

public class ActuatorLanegroupClosure extends AbstractActuator {

    public Set<Commodity> comms;
    public Set<AbstractLaneGroup> lanegroups;
    public boolean ison;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ActuatorLanegroupClosure(Scenario scenario, Actuator jaxb_actuator) throws OTMException {
        super(scenario, jaxb_actuator);
        this.comms = new HashSet<>();
        this.lanegroups = new HashSet<>();
        this.ison = true;
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

    }

}
