package api.info;

import common.Node;

public class NodeInfo {

    /** Integer id of the node. */
    public long id;

    public NodeInfo(Node x){
        this.id = x.getId();
    }

    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "NodeInfo{" +
                "id=" + id +
                '}';
    }

}
