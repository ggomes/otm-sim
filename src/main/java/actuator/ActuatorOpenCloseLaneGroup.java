package actuator;

import common.AbstractLaneGroup;
import common.LaneGroupSet;
import common.Scenario;
import control.command.CommandBoolean;
import control.command.InterfaceCommand;
import error.OTMException;
import jaxb.Actuator;

public class ActuatorOpenCloseLaneGroup extends AbstractActuator {

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ActuatorOpenCloseLaneGroup(Scenario scenario, Actuator jact) throws OTMException {
        super(scenario, jact);
    }

    ///////////////////////////////////////////////////
    // AbstractActuator
    ///////////////////////////////////////////////////

    @Override
    public Type getType() {
        return Type.opencloselg;
    }

    @Override
    public void process_controller_command(InterfaceCommand command, float timestamp) throws OTMException {

        if(command==null)
            return;

        boolean isopen = ((CommandBoolean)command).value;
        for(AbstractLaneGroup lg : ((LaneGroupSet)target).lgs){
            lg.set_actuator_isopen(isopen,commid);
        }
    }

}
