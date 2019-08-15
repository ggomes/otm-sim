package keys;

public class KeyCommodityNodeLink {
    public final long commodity_id;
    public final long node_id;
    public final long linkin_id;
    public KeyCommodityNodeLink(long commodity_id, long node_id, long linkin_id) {
        this.commodity_id = commodity_id;
        this.node_id = node_id;
        this.linkin_id = linkin_id;
    }
}
