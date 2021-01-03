package actuator;

import core.AbstractLaneGroup;
import core.LaneGroupSet;
import core.Scenario;
import control.command.CommandNumber;
import control.command.InterfaceCommand;
import core.ScenarioElementType;
import error.OTMException;
import jaxb.Actuator;
import utils.OTMUtils;

import java.util.Set;

public class ActuatorLaneGroupSpeedLimit extends AbstractActuatorLaneGroup {

    public ActuatorLaneGroupSpeedLimit(Scenario scenario, Actuator jact) throws OTMException {
        super(scenario, jact);
    }

    @Override
    public Type getType() {
        return Type.lg_speedlimit;
    }

    @Override
    public void process_controller_command(InterfaceCommand command, float timestamp) throws OTMException {
        if(command==null)
            return;
        if(!(command instanceof CommandNumber))
            throw new OTMException("Bad command type.");
        double value = ((CommandNumber)command).value;
        for(AbstractLaneGroup lg : lanegroups)
            lg.set_actuator_speed_mps(value);
    }

}
