package actuator;

import core.AbstractLaneGroup;
import core.LaneGroupSet;
import core.Scenario;
import core.ScenarioElementType;
import dispatch.EventPoke;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Actuator;
import utils.OTMUtils;

import java.util.Set;

public abstract class AbstractActuatorLaneGroup extends AbstractActuator {

    private String lgstr;
    protected Set<AbstractLaneGroup> lanegroups;

    public AbstractActuatorLaneGroup(Scenario scenario, Actuator jact) throws OTMException {
        super(scenario, jact);

        jaxb.ActuatorTarget e = jact.getActuatorTarget();
        ScenarioElementType mytype = ScenarioElementType.valueOf(e.getType());
        if(mytype!=ScenarioElementType.lanegroups)
            throw new OTMException("Wrong target type in actuator");
        lgstr = e.getLanegroups();
    }

    @Override
    protected ScenarioElementType get_target_class() {
        return ScenarioElementType.lanegroups;
    }

    @Override
    public void initialize(Scenario scenario, float timestamp, boolean override_targets) throws OTMException {

        if(initialized)
            return;

        super.initialize(scenario, timestamp,override_targets);

        this.target = OTMUtils.read_lanegroups(lgstr,scenario.network.links);
        this.lanegroups  =  ((LaneGroupSet)target).lgs;

        if(lanegroups==null || lanegroups.isEmpty())
            throw new OTMException(String.format("Actuator %d has no lanegroups.",id));

        set_dt_for_target();

        if(dt!=null)
            scenario.dispatcher.register_event(new EventPoke(scenario.dispatcher,3,timestamp,this));

        // register the actuator
        target.register_actuator(commids,this,override_targets);
    }

}
