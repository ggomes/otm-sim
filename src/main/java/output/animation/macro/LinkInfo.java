/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package output.animation.macro;

import common.AbstractLaneGroup;
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
