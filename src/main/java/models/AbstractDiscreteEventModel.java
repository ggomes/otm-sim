package models;

import common.Link;

import java.util.Set;

public abstract class AbstractDiscreteEventModel extends AbstractModel {

    public AbstractDiscreteEventModel(Set<Link> links, String name,boolean is_default) {
        super(links,name,is_default);
        this.model_type = ModelType.discrete_event;
    }

}
