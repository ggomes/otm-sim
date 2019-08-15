package output.animation.macro;

import models.AbstractLaneGroup;
import common.Link;
import output.animation.AbstractLaneGroupInfo;
import output.animation.AbstractLinkInfo;

public class LinkInfo extends AbstractLinkInfo {

    public LinkInfo(Link link) {
        super(link);
    }

    @Override
    public AbstractLaneGroupInfo newLaneGroupInfo(AbstractLaneGroup lg) {
        return new LaneGroupInfo(lg);
    }


}
