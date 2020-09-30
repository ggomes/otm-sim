package actuator;

import control.command.CommandNumber;
import control.command.InterfaceCommand;
import error.OTMException;
import jaxb.Actuator;
import common.Scenario;

/** This actuator controls the maximum flow rate on a contiguous set of lanes
 *  in a link. The control signal is constrained between user-defined maximum
 *  and minimum values.
 */
public class ActuatorMeter extends AbstractActuatorLanegroupCapacity  {

    public float max_rate_vps;
    public float min_rate_vps;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ActuatorMeter(Scenario scenario, Actuator jact) throws OTMException {
        super(scenario, jact);

        // interpret jact.getMaxValue and jact.getMinValue in vphpl
        max_rate_vps = jact.getMaxValue()>=0f ? jact.getMaxValue()*total_lanes/3600f : Float.POSITIVE_INFINITY;
        min_rate_vps = jact.getMinValue()>=0f ? jact.getMinValue()*total_lanes/3600f : 0f;
    }

    ///////////////////////////////////////////////////
    // AbstractActuator
    ///////////////////////////////////////////////////

    @Override
    public Type getType() {
        return Type.meter;
    }

    @Override
    public void process_controller_command(InterfaceCommand command, float timestamp) throws OTMException {
        super.process_controller_command(
                new CommandNumber(command==null?null:Math.max(Math.min(((CommandNumber) command).value,max_rate_vps),min_rate_vps))
                ,timestamp);
    }

}
