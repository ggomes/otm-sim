package events;

import core.Link;
import core.Scenario;
import utils.OTMUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EventLinksToggle extends AbstractEvent {

    Set<Link> links = new HashSet<>();

    public EventLinksToggle(long id, EventType type, float timestamp) {
        super(id, type, timestamp);
    }

    public EventLinksToggle(Scenario scenario, jaxb.Event jev){
        super(jev);
        if(jev.getEventTarget()!=null && jev.getEventTarget().getIds()!=null){
            links = OTMUtils.csv2longlist(jev.getEventTarget().getIds()).stream()
                    .filter(id->scenario.network.links.containsKey(id))
                    .map(id->scenario.network.links.get(id))
                    .collect(Collectors.toSet());
        }
    }

}
