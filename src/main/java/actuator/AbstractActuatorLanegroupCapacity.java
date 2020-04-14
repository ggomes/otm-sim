package actuator;

import common.Link;
import common.Scenario;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Actuator;
import common.AbstractLaneGroup;
import utils.OTMUtils;

import java.util.HashSet;
import java.util.Set;

/** This is an abstract class for actuators whose target is
 * a set of lane group capacities.
 * The command is a single rate for all lane groups in veh/sec
 */
public abstract class AbstractActuatorLanegroupCapacity extends AbstractActuator {

    protected Set<AbstractLaneGroup> lanegroups;
    public final int total_lanes;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public AbstractActuatorLanegroupCapacity(Scenario scenario, Actuator jaxb_actuator) throws OTMException {
        super(scenario, jaxb_actuator);

        if(jaxb_actuator.getActuatorTarget()!=null) {
            jaxb.ActuatorTarget e = jaxb_actuator.getActuatorTarget();
            if ( e.getType().equalsIgnoreCase("link") ) {

                long link_id = e.getId();
                if(!scenario.network.links.containsKey(link_id))
                    throw new OTMException("Unknown link id in actuator " + id );
                Link link = scenario.network.links.get(link_id);

                int [] x = OTMUtils.read_lanes(e.getLanes(),link.full_lanes);
                int start_lane = x[0];
                int end_lane = x[1];

                this.lanegroups = link.get_unique_lanegroups_for_dn_lanes(start_lane, end_lane);
                this.total_lanes = end_lane-start_lane+1;

            } else {
                throw new OTMException("Unknown actuator type '" + e.getType() + "'");
            }
        } else {
            this.lanegroups = new HashSet<>();
            this.total_lanes = 0;
        }
    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void validate(OTMErrorLog errorLog) {

        // check that the lanes provided cover all lane, ie that total lanes
        // equals the sum of lanes in the lanegroups
        int lg_lanes = lanegroups.stream().mapToInt(x->x.num_lanes).sum();
        if(lg_lanes!=total_lanes)
            errorLog.addError("A lane group actuator must exactly cover its lane groups");
    }

    ///////////////////////////////////////////////////
    // AbstractActuator
    ///////////////////////////////////////////////////

    @Override
    public void process_controller_command(Object command, float timestamp) throws OTMException {
        if(command==null)
            return;
        float rate_vps = (float) command;
        for(AbstractLaneGroup lg : lanegroups)
            lg.set_actuator_capacity_vps(rate_vps * lg.num_lanes / total_lanes);
    }

}
