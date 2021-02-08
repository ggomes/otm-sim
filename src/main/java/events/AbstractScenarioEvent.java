package events;

import core.Scenario;
import dispatch.AbstractEvent;
import error.OTMErrorLog;
import error.OTMException;

public abstract class AbstractScenarioEvent extends AbstractEvent {

    public enum EventType {linktgl, acttgl,lglanes,lgfd};

    public final long id;
    public final EventType type;
    public final String name;

    public AbstractScenarioEvent(long id, EventType type,float timestamp,String name) {
        super(null,0,timestamp,null);
        this.id = id;
        this.type = type;
        this.name = name;
    }

    public AbstractScenarioEvent(jaxb.Event jev){
        this(jev.getId(),EventType.valueOf(jev.getType()),jev.getTimestamp(),jev.getName());
    }

    public void validate_pre_init(OTMErrorLog errorLog){
        if(this.type==null)
            errorLog.addError("Bad event type");
        if(this.timestamp<0)
            errorLog.addError("Negative time stamp in event");
    }

    public void initialize(Scenario scenario) throws OTMException {
        this.dispatcher = scenario.dispatcher;
    }

    public abstract void validate_post_init(OTMErrorLog errorLog);
}
