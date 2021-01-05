package dispatch;

import error.OTMException;
import output.AbstractOutputTimed;

public class EventTimedWrite extends AbstractEvent {

    public EventTimedWrite(Dispatcher dispatcher,float timestamp,Object obj){
        super(dispatcher,70,timestamp,obj);
    }

    @Override
    public void action() throws OTMException {
        AbstractOutputTimed obj = (AbstractOutputTimed)recipient;
        obj.write(timestamp);
        dispatcher.register_event(new EventTimedWrite(dispatcher,timestamp + obj.outDt,recipient));
    }
}
