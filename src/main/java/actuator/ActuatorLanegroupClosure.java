package actuator;

import common.AbstractLaneGroup;
import common.LaneGroupSet;
import common.Scenario;
import control.command.CommandNumber;
import control.command.CommandOpenClosed;
import control.command.InterfaceCommand;
import control.commodity.ControllerLanegroupClosure;
import error.OTMException;
import jaxb.Actuator;

import java.util.Set;

public class ActuatorLanegroupClosure extends AbstractActuator {

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ActuatorLanegroupClosure(Scenario scenario, Actuator jaxb_actuator) throws OTMException {
        super(scenario, jaxb_actuator);
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
    public Type getType() {
        return Type.lanegroupclosure;
    }

    @Override
    public void process_controller_command(InterfaceCommand command, float timestamp) throws OTMException {
        if(command==null)
            return;
        boolean isopen = command==CommandOpenClosed.open;
        Set<Long> commids = ((ControllerLanegroupClosure)this.myController).commids;

        for(AbstractLaneGroup lg : ((LaneGroupSet)this.target).lgs)
            lg.set_actuator_isopen(isopen,commids);
    }

}
