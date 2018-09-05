package dispatch;

import error.OTMException;
import output.AbstractOutputTimed;

public class EventTimedWrite extends AbstractEvent {

    public EventTimedWrite(Dispatcher dispatcher,float timestamp,Object obj){
        super(dispatcher,2,timestamp,obj);
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        ((AbstractOutputTimed)recipient).write(timestamp,null);
    }
}
