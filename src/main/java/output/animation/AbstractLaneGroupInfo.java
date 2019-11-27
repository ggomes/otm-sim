package output.animation;

import models.BaseLaneGroup;

public abstract class AbstractLaneGroupInfo implements InterfaceLaneGroupInfo {
    public Long lg_id;

    public AbstractLaneGroupInfo(BaseLaneGroup lg) {
        this.lg_id = lg.id;
    }

}
