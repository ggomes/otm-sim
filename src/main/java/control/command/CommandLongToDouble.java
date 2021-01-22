package control.command;

import java.util.HashMap;
import java.util.Map;

public class CommandLongToDouble implements InterfaceCommand {

    public Map<Long,Double> X = new HashMap<>();

    @Override
    public String asString() {
        return null;
    }
}
