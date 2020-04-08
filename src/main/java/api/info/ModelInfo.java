package api.info;

import models.AbstractModel;
import models.fluid.AbstractFluidModel;

import java.util.Set;
import java.util.stream.Collectors;

public class ModelInfo {

    public Set<Long> link_ids;
    public String name;
    public float dt;

    public ModelInfo(AbstractModel model){
        this.link_ids = model.links.stream().map(link->link.getId()).collect(Collectors.toSet());
        this.name = model.name;
        this.dt = (model instanceof AbstractFluidModel) ?  ((AbstractFluidModel) model).dt_sec : Float.NaN;
    }

    public Set<Long> getLink_ids() {
        return link_ids;
    }

    public String getName() {
        return name;
    }

    public float getDt() {
        return dt;
    }

}
