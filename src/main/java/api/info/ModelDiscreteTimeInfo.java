package api.info;

public class ModelDiscreteTimeInfo extends ModelInfo {

    public float dt;
    public float max_cell_length;

    public ModelDiscreteTimeInfo(models.ctm.Model_CTM model) {
        super(model);
        this.dt = model.dt;
        this.max_cell_length = model.max_cell_length;
    }
}
