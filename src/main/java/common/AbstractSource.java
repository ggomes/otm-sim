package common;

import commodity.Commodity;
import commodity.Path;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import keys.KeyCommPathOrLink;
import profiles.DemandProfile;

public abstract class AbstractSource {

    public Link link;
    public DemandProfile profile;   // profile that created this source
    public KeyCommPathOrLink key;
    protected Commodity commodity;

    // demand value
    protected double source_demand;    // vps

    public AbstractSource(Link link, DemandProfile profile, Commodity commodity, Path path){
        this.link = link;
        this.profile = profile;
        this.commodity = commodity;
        this.key = new KeyCommPathOrLink(commodity,path,link);
        this.source_demand = 0f;
    }

    public void delete(){
        link = null;
        profile = null;
        profile = null;
        key = null;
    }

    public void validate(OTMErrorLog errorLog) {
//        if(link.is_sink)
//            errorLog.addError("source cannot be placed on a sink link.");
    }

    public void set_demand_in_veh_per_timestep(Dispatcher dispatcher, float time, double value) throws OTMException {
        source_demand = value;
    }

    public double get_value_in_veh_per_timestep(){
        return source_demand;
    }

}
