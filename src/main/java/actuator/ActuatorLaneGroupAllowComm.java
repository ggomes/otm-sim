package actuator;

import core.AbstractLaneGroup;
import core.Scenario;
import control.command.CommandRestrictionMap;
import control.command.InterfaceCommand;
import control.commodity.ControllerRestrictLaneGroup;
import error.OTMException;
import jaxb.Actuator;

import java.util.Map;

public class ActuatorLaneGroupAllowComm extends AbstractActuatorLaneGroup {

    public ActuatorLaneGroupAllowComm(Scenario scenario, Actuator jact) throws OTMException {
        super(scenario, jact);
        this.dt=0f; // this is changed to simdt or null at by AbstractActuator.initialize.
    }

    @Override
    public Type getType() {
        return Type.lg_allowcomm;
    }

    @Override
    public void process_command(InterfaceCommand command, float timestamp) throws OTMException {

        if(command==null)
            return;
        if(!(command instanceof CommandRestrictionMap))
            throw new OTMException("Bad command type.");

        for(Map.Entry<Long, ControllerRestrictLaneGroup.Restriction> e : ((CommandRestrictionMap)command).values.entrySet()) {
            Long commid = e.getKey();
            boolean allow = e.getValue()== ControllerRestrictLaneGroup.Restriction.Open;
            for (AbstractLaneGroup lg : lanegroups)
                lg.set_actuator_allow_comm(allow, commid);
        }
    }

    @Override
    protected InterfaceCommand command_off() {
        return null;
    }

}
