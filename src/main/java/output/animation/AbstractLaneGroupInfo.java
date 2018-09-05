package output.animation;

import common.AbstractLaneGroup;

public abstract class AbstractLaneGroupInfo implements InterfaceLaneGroupInfo {
    public Long lg_id;

    public AbstractLaneGroupInfo(AbstractLaneGroup lg) {
        this.lg_id = lg.id;
    }
}
