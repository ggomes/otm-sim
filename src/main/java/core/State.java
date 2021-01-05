package core;

import commodity.Commodity;
import commodity.Path;

import java.util.Objects;

public class State implements Comparable<State> {

    public final long commodity_id;
    public final long pathOrlink_id;    // id of either a link or a path
    public final boolean isPath;        // true is pathOrlink_id is path, false otherwise

    public State(Commodity comm, Path path, Link link) {
        this.commodity_id = comm.getId();
        this.isPath = comm.pathfull;
        this.pathOrlink_id = isPath ? path.getId() : link.getId();
    }

    public State(long commodity_id, long pathOrlink_id, boolean isPath) {
        this.commodity_id = commodity_id;
        this.pathOrlink_id = pathOrlink_id;
        this.isPath = isPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State that = (State) o;
        return commodity_id == that.commodity_id &&
                pathOrlink_id == that.pathOrlink_id &&
                isPath == that.isPath;
    }

    @Override
    public int hashCode() {
        return Objects.hash(commodity_id, pathOrlink_id, isPath);
    }

    @Override
    public int compareTo(State that) {

        if(this.isPath && !that.isPath)
            return 1;
        if(!this.isPath && that.isPath)
            return -1;

        if(this.commodity_id>that.commodity_id)
            return 1;
        if(this.commodity_id<that.commodity_id)
            return -1;

        if(this.pathOrlink_id>that.pathOrlink_id)
            return 1;
        if(this.pathOrlink_id<that.pathOrlink_id)
            return -1;

        return 0;
    }
}
