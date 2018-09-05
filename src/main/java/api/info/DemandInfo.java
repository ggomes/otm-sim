package api.info;

import jaxb.Demand;
import keys.DemandType;
import profiles.AbstractDemandProfile;
import profiles.DemandProfile;
import profiles.Profile1D;

public class DemandInfo {

    /** Either "pathless" or "pathfull" */
    public String type;

    /** Integer id of the source link, if the demand is for a pathless commodity. */
    public long link_id;

    /** Integer id of the path (subnetwork), if the demand is for a pathfull commodity. */
    public long path_id;

    /** Integer id of the commodity. */
    public Long commodity_id;

    /** Time series of demand values in vehicles per second. */
    public Profile1DInfo profile;       // rate in veh/sec

    public DemandInfo(AbstractDemandProfile x){
        this.type = x.get_type().toString();
        this.commodity_id = x.commodity!=null? x.commodity.getId() : null;
        if(x.profile!=null)
            this.profile = new Profile1DInfo(x.profile);

        if(!(x instanceof DemandProfile))
            return;

        DemandProfile dp = (DemandProfile) x;
        if(x.get_type()==DemandType.pathless)
            this.link_id = dp.link.getId();
        if(x.get_type()==DemandType.pathfull)
            this.path_id = dp.path.getId();
    }

    public String getType() {
        return type;
    }

    public long getLink_id() {
        return link_id;
    }

    public long getPath_id() {
        return path_id;
    }

    public Long getCommodity_id() {
        return commodity_id;
    }

    public Profile1DInfo getProfile(){
        return profile;
    }

    @Override
    public String toString() {
        String str = "DemandInfo{" +
                "type=" + type +
                ", commodity_id=" + commodity_id;
        switch(type){
            case "pathfull":
                str += ", path_id=" + path_id;
                break;
            case "pathless":
                str += ", link_id=" + link_id;
                break;
        }
        str += ", profile=" + profile + '}';
        return str;
    }
}
