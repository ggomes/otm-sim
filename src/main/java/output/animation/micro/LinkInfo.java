package output.animation.micro;

import core.AbstractLaneGroup;
import core.Link;
import output.animation.AbstractLaneGroupInfo;
import output.animation.AbstractLinkInfo;

public class LinkInfo extends AbstractLinkInfo {

    public LinkInfo(Link link) {
        super(link);
    }

    @Override
    public AbstractLaneGroupInfo newLaneGroupInfo(AbstractLaneGroup lg) {
        return null;
    }

}
