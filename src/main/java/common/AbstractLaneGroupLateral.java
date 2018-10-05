package common;

import geometry.Side;

public abstract class AbstractLaneGroupLateral extends AbstractLaneGroup {

    public AbstractLaneGroupLateral(Link link, Side side, float length, int num_lanes, int start_lane) {
        super(link,side,length,num_lanes);
        this.start_lane_up = start_lane;
    }
}
