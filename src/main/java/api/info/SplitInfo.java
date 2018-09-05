package api.info;

import profiles.Profile2D;
import profiles.SplitMatrixProfile;

public class SplitInfo {

    /** Integer id of the commodity. */
    public long commodity_id;

    /** Integer id of the node. */
    public long node_id;

    /** Integer id of the upstream link. */
    public long link_in_id;

    /** Matrix of split values indexed by downstream link and time. */
    public Profile2D splits;

    public SplitInfo(SplitMatrixProfile x){
        this.commodity_id = x.commodity_id;
        this.node_id = x.node.getId();
        this.link_in_id = x.link_in_id;
        this.splits = x.clone_splits();
    }

    public long getCommodity_id() {
        return commodity_id;
    }

    public long getNode_id() {
        return node_id;
    }

    public long getLink_in_id() {
        return link_in_id;
    }

    @Override
    public String toString() {
        return "SplitInfo{" +
                "commodity_id=" + commodity_id +
                ", node_id=" + node_id +
                ", link_in_id=" + link_in_id +
                ", splits=" + splits +
                '}';
    }
}
