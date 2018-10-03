package common;

import java.util.Set;

public abstract class AbstractLaneGroupLateral extends AbstractLaneGroup {

    public AbstractLaneGroupLateral(Link link, Set<Integer> lanes, Set<RoadConnection> out_rcs) {
        super(link, lanes, out_rcs);
    }


}
