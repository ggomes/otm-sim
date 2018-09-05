package keys;

public class KeyCommoditySourceSink {

    public final long commodity_id;
    public final long source_id;
    public final long sink_id;

    public KeyCommoditySourceSink(long commodity_id, long source_id, long sink_id) {
        this.commodity_id = commodity_id;
        this.source_id = source_id;
        this.sink_id = sink_id;
    }
}
