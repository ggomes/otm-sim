package lanechange;

import models.Maneuver;

import java.util.Set;

public interface InterfaceLaneSelector {
    void update_lane_change_probabilities_with_options(Long pathorlinkid, Set<Maneuver> lcoptions);

}
