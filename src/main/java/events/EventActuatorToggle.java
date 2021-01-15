package events;

import actuator.AbstractActuator;
import core.Scenario;
import error.OTMErrorLog;
import error.OTMException;
import utils.OTMUtils;

import java.util.Set;
import java.util.stream.Collectors;

public class EventActuatorToggle extends AbstractScenarioEvent {

    Set<AbstractActuator> actuators;
    boolean ison;

    public EventActuatorToggle(long id, EventType type, float timestamp) {
        super(id, type, timestamp);
    }

    public EventActuatorToggle(Scenario scenario, jaxb.Event jev){
        super(jev);
        if(jev.getEventTarget()!=null && jev.getEventTarget().getIds()!=null){
            actuators = OTMUtils.csv2longlist(jev.getEventTarget().getIds()).stream()
                    .filter(id->scenario.actuators.containsKey(id))
                    .map(id->scenario.actuators.get(id))
                    .collect(Collectors.toSet());
        }

        this.ison = true;
        if(jev.getParameters()!=null)
            for(jaxb.Parameter p : jev.getParameters().getParameter())
                if(p.getName().equals("ison"))
                    this.ison = Boolean.parseBoolean(p.getValue());

    }

    @Override
    public void validate_pre_init(OTMErrorLog errorLog) {
        super.validate_pre_init(errorLog);
        if(actuators.contains(null))
            errorLog.addError("Bad actuator id in event");
    }

    @Override
    public void validate_post_init(OTMErrorLog errorLog) {
    }

    @Override
    public void action() throws OTMException {
        System.out.println(String.format("%.2f\t%s\t%b",timestamp,getClass().getName(),ison));

        if(ison)
            for(AbstractActuator act : actuators)
                act.turn_on();
        else
            for(AbstractActuator act : actuators)
                act.turn_off();
    }
}
