package models;

public abstract class AbstractDiscreteEventModel extends AbstractModel {

    public AbstractDiscreteEventModel(String name,boolean is_default) {
        super(name,is_default);
        this.model_type = ModelType.discrete_event;
    }

}
