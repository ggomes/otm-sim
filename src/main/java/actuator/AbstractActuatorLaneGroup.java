package actuator;

import core.AbstractLaneGroup;
import core.LaneGroupSet;
import core.Scenario;
import core.ScenarioElementType;
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
    public void initialize(Scenario scenario, float start_time) throws OTMException {
        super.initialize(scenario, start_time);

        this.target = OTMUtils.read_lanegroups(lgstr,scenario.network);
        this.lanegroups  =  ((LaneGroupSet)target).lgs;

        if(lanegroups==null || lanegroups.isEmpty())
            throw new OTMException(String.format("Actuator %d has no lanegroups.",id));

        // register the actuator
        target.register_actuator(commids,this);
    }

}
