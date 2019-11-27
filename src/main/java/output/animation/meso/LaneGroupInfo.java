package output.animation.meso;

import models.BaseLaneGroup;
import output.animation.AbstractLaneGroupInfo;

public class LaneGroupInfo extends AbstractLaneGroupInfo {

    public LaneGroupInfo(BaseLaneGroup lg) {
        super(lg);
    }

    @Override
    public Double get_total_vehicles() {
        return null;
    }

}
