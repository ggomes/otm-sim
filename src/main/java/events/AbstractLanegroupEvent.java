package events;

import core.AbstractLaneGroup;
import core.Scenario;
import error.OTMErrorLog;
import error.OTMException;
import utils.OTMUtils;

import java.util.Set;

public abstract class AbstractLanegroupEvent extends AbstractScenarioEvent {

    String lgstring;
    Set<AbstractLaneGroup> lanegroups;

    public AbstractLanegroupEvent(long id, EventType type, float timestamp,String name) {
        super(id, type, timestamp,name);
    }

    public AbstractLanegroupEvent(Scenario scenario, jaxb.Event jev) throws OTMException {
        super(jev);
        this.lgstring = jev.getEventTarget().getLanegroups();

        this.lgstring = "";
        if(jev.getEventTarget()!=null && jev.getEventTarget().getLanegroups()!=null)
            this.lgstring = jev.getEventTarget().getLanegroups();

    }

    @Override
    public void validate_post_init(OTMErrorLog errorLog) {
        if(lanegroups.contains(null))
            errorLog.addError("Bad lanegroup in lanegroup event");
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
        this.lanegroups = OTMUtils.read_lanegroups(this.lgstring,scenario.network.links).lgs;
    }
}
