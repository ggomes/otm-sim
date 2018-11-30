/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package output.animation;

import models.AbstractLaneGroup;

public abstract class AbstractLaneGroupInfo implements InterfaceLaneGroupInfo {
    public Long lg_id;

    public AbstractLaneGroupInfo(AbstractLaneGroup lg) {
        this.lg_id = lg.id;
    }

}
