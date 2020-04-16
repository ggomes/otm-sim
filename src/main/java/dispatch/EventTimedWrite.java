package dispatch;

import error.OTMException;
import output.AbstractOutputTimed;

public class EventTimedWrite extends AbstractEvent {

    public EventTimedWrite(Dispatcher dispatcher,float timestamp,Object obj){
        super(dispatcher,7,timestamp,obj);
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        AbstractOutputTimed obj = (AbstractOutputTimed)recipient;
        obj.write(timestamp);
        dispatcher.register_event(new EventTimedWrite(dispatcher,timestamp + obj.outDt,recipient));
    }
}
