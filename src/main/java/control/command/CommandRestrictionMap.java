package control.command;

import control.commodity.ControllerRestrictLaneGroup;

import java.util.Map;

public class CommandRestrictionMap implements InterfaceCommand {

    public Map<Long, ControllerRestrictLaneGroup.Restriction> values;

    public CommandRestrictionMap(Map<Long, ControllerRestrictLaneGroup.Restriction> x) {
        values = x;
    }

    @Override
    public String asString() {
        return values.toString();
    }
}
