package control.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public Map<Long,Double> as_map(){
        Map<Long,Double> x = new HashMap<>();
        for(int i=0;i<ids.length;i++)
            x.put(ids[i],values[i]);
        return x;
    }

    @Override
    public String asString() {
        return values.toString();
    }
}
