package actuator;

import control.command.CommandNumber;
import control.command.InterfaceCommand;
import core.AbstractLaneGroup;
import core.LaneGroupSet;
import core.ScenarioElementType;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Actuator;
import core.Scenario;
import utils.OTMUtils;

import java.util.Set;

/** This actuator controls the maximum flow rate on a contiguous set of lanes
 *  in a link. The control signal is constrained between user-defined maximum
 *  and minimum values.
 */
public class ActuatorLaneGroupCapacity extends AbstractActuatorLaneGroup  {

    private float jmax_rate;
    private float jmin_rate;

    public int total_lanes;
    public float max_rate_vps;
    public float min_rate_vps;

    public ActuatorLaneGroupCapacity(Scenario scenario, Actuator jact) throws OTMException {
        super(scenario, jact);
        jmax_rate = jact.getMaxValue()==null ? jmax_rate=Float.POSITIVE_INFINITY : jact.getMaxValue();
        jmin_rate = jact.getMinValue()==null ? jmin_rate=Float.NEGATIVE_INFINITY : jact.getMinValue();
    }

    @Override
    public Type getType() {
        return Type.lg_capacity;
    }

    @Override
    public void initialize(Scenario scenario, float start_time,boolean override_targets) throws OTMException {

        if(initialized)
            return;

        super.initialize(scenario, start_time,override_targets);

        this.total_lanes = lanegroups==null || lanegroups.isEmpty() ? 0 : lanegroups.stream().mapToInt(x->x.get_num_lanes()).sum();

        // interpret jact.getMaxValue and jact.getMinValue in vphpl
        max_rate_vps = jmax_rate>=0f ? jmax_rate*total_lanes/3600f : Float.POSITIVE_INFINITY;
        min_rate_vps = jmin_rate>=0f ? jmin_rate*total_lanes/3600f : 0f;

        // check that the lanes provided cover all lane, ie that total lanes
        // equals the sum of lanes in the lanegroups
        int lg_lanes = lanegroups.stream().mapToInt(x->x.get_num_lanes()).sum();
        if(lg_lanes!=total_lanes)
            throw new OTMException("A lane group actuator must exactly cover its lane groups");

    }

    @Override
    public void process_command(InterfaceCommand command, float timestamp) throws OTMException {
        if(command==null)
            return;
        if(!(command instanceof CommandNumber))
            throw new OTMException("Bad command type.");
        Float rate_vps = Math.max(Math.min(((CommandNumber) command).value,max_rate_vps),min_rate_vps);
        if(rate_vps==null)
            return;
        for(AbstractLaneGroup lg : lanegroups)
            lg.set_actuator_capacity_vps(rate_vps * lg.get_num_lanes() / total_lanes);
    }

    @Override
    protected InterfaceCommand command_off() {
        return new CommandNumber(Float.POSITIVE_INFINITY);
    }

}
