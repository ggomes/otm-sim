package control.command;

import actuator.SignalPhase;

import java.util.Map;

public class CommandSignal implements InterfaceCommand {

    public Map<Long, SignalPhase.BulbColor> value;  // phase,color

    public CommandSignal(Map<Long, SignalPhase.BulbColor> value) {
        this.value = value;
    }

    @Override
    public String asString() {
        if(value.isEmpty())
            return "{}";
        String str = "";
        for(Map.Entry<Long, SignalPhase.BulbColor> e : value.entrySet())
            str += String.format("%d:'%s',",e.getKey(),e.getValue());
        return "{" + str.substring(0,str.length()-1) + "}";
    }

}
