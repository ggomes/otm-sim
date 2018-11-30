/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package output.animation.micro;

import models.AbstractLaneGroup;
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
