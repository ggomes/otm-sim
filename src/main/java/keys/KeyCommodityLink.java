package keys;

public class KeyCommodityLink {

    public final long commodity_id;
    public final long link_id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KeyCommodityLink that = (KeyCommodityLink) o;

        if (commodity_id != that.commodity_id) return false;
        return link_id == that.link_id;
    }

    @Override
    public int hashCode() {
        int result = (int) (commodity_id ^ (commodity_id >>> 32));
        result = 31 * result + (int) (link_id ^ (link_id >>> 32));
        return result;
    }

    public KeyCommodityLink(long commodity_id, long link_id) {
        this.commodity_id = commodity_id;
        this.link_id = link_id;
    }

}
