package control.command;

import java.util.Map;

public class CommandDoubleMap implements InterfaceCommand {

    public Map<Long, Double> values;

    public CommandDoubleMap(Map<Long, Double> x) {
        values = x;
    }

    @Override
    public String asString() {
        return values.toString();
    }
}
