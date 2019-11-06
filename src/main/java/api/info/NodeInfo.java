package api.info;

import common.Node;

public class NodeInfo {

    /** Integer id of the node. */
    public long id;
    public float x;
    public float y;

    public NodeInfo(Node node){
        this.id = node.getId();
        this.x = node.xcoord;
        this.y = node.ycoord;
    }

    public long getId() {
        return id;
    }
    public float getX(){ return x; }
    public float getY(){ return y; }

    @Override
    public String toString() {
        return "NodeInfo{" +
                "id=" + id +
                '}';
    }

}
