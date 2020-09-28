package actuator;

import common.AbstractLaneGroup;
import common.LaneGroupSet;
import common.Scenario;
import control.command.CommandOpenClosed;
import control.command.InterfaceCommand;
import control.commodity.ControllerLanegroupRestrict;
import error.OTMException;
import jaxb.Actuator;

import java.util.Set;

public class ActuatorLanegroupRestrict extends AbstractActuator {

    protected Set<AbstractLaneGroup> lanegroups;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ActuatorLanegroupRestrict(Scenario scenario, Actuator jact) throws OTMException {
        super(scenario, jact);
        this.lanegroups = read_lanegroups(scenario,jact);
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

        System.out.println(String.format("%.1f\tActuatorLanegroupRestrict\tprocess_controller_command",timestamp));

        boolean isopen = command==CommandOpenClosed.open;
        Set<Long> commids = ((ControllerLanegroupRestrict)this.myController).commids;

//        for(AbstractLaneGroup lg : ((LaneGroupSet)this.target).lgs)
//            lg.set_actuator_isopen(isopen,commids);
    }

}
