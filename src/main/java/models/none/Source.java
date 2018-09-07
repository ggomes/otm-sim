/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.none;

import commodity.Commodity;
import commodity.Path;
import common.AbstractSource;
import common.Link;
import profiles.DemandProfile;

public class Source extends AbstractSource {

    public Source(Link link, DemandProfile profile, Commodity commodity, Path path) {
        super(link, profile, commodity, path);
    }

}
