package models;

import common.Link;

import java.util.Set;

public abstract class AbstractDiscreteEventModel extends AbstractModel {

    public AbstractDiscreteEventModel(Set<Link> links, String name) {
        super(links,name);
    }

}
