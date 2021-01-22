package control.command;

import java.util.List;

public class CommandDoubleArray implements InterfaceCommand {

    public long[] ids;
    public double[] values;

    public CommandDoubleArray(long[] ids) {
        this.ids = ids;
        values = new double[ids.length];
    }

    public CommandDoubleArray(List<Long> aids) {
        this.ids = aids.stream().mapToLong(l -> l).toArray();
        values = new double[aids.size()];
    }

    @Override
    public String asString() {
        return values.toString();
    }
}
