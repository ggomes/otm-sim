package output.animation.macro;

import models.BaseLaneGroup;
import common.Link;
import output.animation.AbstractLaneGroupInfo;
import output.animation.AbstractLinkInfo;

public class LinkInfo extends AbstractLinkInfo {

    public LinkInfo(Link link) {
        super(link);
    }

    @Override
    public AbstractLaneGroupInfo newLaneGroupInfo(BaseLaneGroup lg) {
        return new LaneGroupInfo(lg);
    }


}
