package actuator;

import common.AbstractLaneGroup;
import common.LaneGroupSet;
import common.Scenario;
import control.command.CommandRestrictionMap;
import control.command.InterfaceCommand;
import control.commodity.ControllerRestrictLaneGroup;
import error.OTMException;
import jaxb.Actuator;

import java.util.Map;

public class ActuatorOpenCloseLaneGroup extends AbstractActuator {

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ActuatorOpenCloseLaneGroup(Scenario scenario, Actuator jact) throws OTMException {
        super(scenario, jact);
        this.dt=0f;
    }

    ///////////////////////////////////////////////////
    // AbstractActuator
    ///////////////////////////////////////////////////

    @Override
    public Type getType() {
        return Type.lg_restrict;
    }

    @Override
    public void process_controller_command(InterfaceCommand command, float timestamp) throws OTMException {

        if(command==null)
            return;

        Map<Long, ControllerRestrictLaneGroup.Restriction> X = ((CommandRestrictionMap)command).values;
        for(Map.Entry<Long, ControllerRestrictLaneGroup.Restriction> e : ((CommandRestrictionMap)command).values.entrySet()) {
            Long commid = e.getKey();
            boolean isopen = e.getValue()== ControllerRestrictLaneGroup.Restriction.Open;
            for (AbstractLaneGroup lg : ((LaneGroupSet) target).lgs) {
                lg.set_actuator_isopen(isopen, commid);
            }
        }
    }

}
