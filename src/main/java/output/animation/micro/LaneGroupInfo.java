package output.animation.micro;

import core.AbstractLaneGroup;
import output.animation.AbstractLaneGroupInfo;

public class LaneGroupInfo extends AbstractLaneGroupInfo {

    public LaneGroupInfo(AbstractLaneGroup lg) {
        super(lg);
    }

    @Override
    public Double get_total_vehicles() {
        return null;
    }
}
