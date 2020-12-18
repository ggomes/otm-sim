package lanechange;

import core.AbstractLaneGroup;
import core.State;

public interface InterfaceLaneSelector {
    void update_lane_change_probabilities_with_options(AbstractLaneGroup lg, State state);
}
