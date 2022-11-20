package output.events;

import control.command.InterfaceCommand;

import java.util.Map;

public class EventWrapperController extends AbstractEventWrapper {
    Map<Long,InterfaceCommand> command;

    public EventWrapperController(float timestamp, Map<Long, InterfaceCommand> command) {
        super(timestamp);
        this.command = command;
    }

    @Override
    public String asString() {
        if(command.isEmpty())
            return "{}";
        String str = "";
        for(Map.Entry<Long,InterfaceCommand> e : command.entrySet())
            str += String.format("%d:%s,",e.getKey(),e.getValue()==null?"":e.getValue().asString());
        return "{" + str.substring(0,str.length()-1) + "}";
    }
}
