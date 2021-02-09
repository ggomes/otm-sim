package events;

import core.AbstractLaneGroup;
import core.Link;
import core.Scenario;
import error.OTMErrorLog;
import error.OTMException;
import utils.OTMUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class EventLinksToggle extends AbstractScenarioEvent {

    Set<Link> links = new HashSet<>();
    boolean isopen;

    public EventLinksToggle(long id, EventType type, float timestamp,String name) {
        super(id, type, timestamp,name);
    }

    public EventLinksToggle(Scenario scenario, jaxb.Event jev){
        super(jev);
        if(jev.getEventTarget()!=null && jev.getEventTarget().getIds()!=null){
            links = OTMUtils.csv2longlist(jev.getEventTarget().getIds()).stream()
                    .filter(id->scenario.network.links.containsKey(id))
                    .map(id->scenario.network.links.get(id))
                    .collect(Collectors.toSet());
        }

        this.isopen = true;
        if(jev.getParameters()!=null)
            for(jaxb.Parameter p : jev.getParameters().getParameter())
                if(p.getName().equals("isopen"))
                    this.isopen = Boolean.parseBoolean(p.getValue());
    }

    @Override
    public void validate_pre_init(OTMErrorLog errorLog) {
        super.validate_pre_init(errorLog);

        if(links.contains(null))
            errorLog.addError("Bad link id in event");
    }

    @Override
    public void validate_post_init(OTMErrorLog errorLog) {

    }

    @Override
    public void action() throws OTMException {
        if(isopen)
            for(Link link : links)
                for(AbstractLaneGroup lg : link.get_lgs())
                    lg.set_to_nominal_capacity();

        else {
            for(Link link : links)
                for(AbstractLaneGroup lg : link.get_lgs())
                    lg.set_actuator_capacity_vps(0f);
        }
    }
}
