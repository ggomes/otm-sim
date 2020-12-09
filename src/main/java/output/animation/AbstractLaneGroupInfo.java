package output.animation;

import core.AbstractLaneGroup;

public abstract class AbstractLaneGroupInfo implements InterfaceLaneGroupInfo {
    public Long lg_id;

    public AbstractLaneGroupInfo(AbstractLaneGroup lg) {
        this.lg_id = lg.id;
    }

}
