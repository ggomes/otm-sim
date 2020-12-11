package control.command;

public class CommandDoubleArray implements InterfaceCommand {

    public long[] ids;
    public double[] values;

    public CommandDoubleArray(long[] ids) {
        this.ids = ids;
        values = new double[ids.length];
    }

    @Override
    public String asString() {
        return values.toString();
    }
}
