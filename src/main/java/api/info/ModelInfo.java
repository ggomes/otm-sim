package api.info;

import models.AbstractModel;

import java.util.Set;
import java.util.stream.Collectors;

public abstract class ModelInfo {

    public AbstractModel.ModelType model_type;
    public Set<Long> link_ids;
    public String name;
    public boolean is_default;

    public ModelInfo(AbstractModel model){
        this.model_type = model.model_type;
        this.link_ids = model.links.stream().map(link->link.getId()).collect(Collectors.toSet());
        this.name = model.name;
        this.is_default = model.is_default;
    }

}
