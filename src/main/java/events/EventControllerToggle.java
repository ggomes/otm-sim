package events;

import control.AbstractController;
import core.Scenario;
import utils.OTMUtils;

import java.util.Set;
import java.util.stream.Collectors;

public class EventControllerToggle extends AbstractEvent {

    Set<AbstractController> controllers;

    public EventControllerToggle(long id, EventType type, float timestamp) {
        super(id, type, timestamp);
    }

    public EventControllerToggle(Scenario scenario, jaxb.Event jev){
        super(jev);
        if(jev.getEventTarget()!=null && jev.getEventTarget().getIds()!=null){
            controllers = OTMUtils.csv2longlist(jev.getEventTarget().getIds()).stream()
                    .filter(id->scenario.controllers.containsKey(id))
                    .map(id->scenario.controllers.get(id))
                    .collect(Collectors.toSet());
        }
    }
}
