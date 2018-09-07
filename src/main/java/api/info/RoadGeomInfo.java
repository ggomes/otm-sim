/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package api.info;

import geometry.RoadGeometry;

public class RoadGeomInfo {

    /** Integer id for this road geometry. */
    public long id;

    /** Upstream left side additional lanes. */
    public AddLanesInfo up_left;

    /** Upstream right side additional lanes. */
    public AddLanesInfo up_right;

    /** Downstream left side additional lanes. */
    public AddLanesInfo dn_left;

    /** Downstream right side additional lanes. */
    public AddLanesInfo dn_right;

    public RoadGeomInfo(RoadGeometry x){

        this.id = x.id;

        if(x.up_left!=null)
            up_left = new AddLanesInfo(x.up_left);

        if(x.up_right!=null)
            up_right = new AddLanesInfo(x.up_right);

        if(x.dn_left!=null)
            dn_left = new AddLanesInfo(x.dn_left);

        if(x.dn_right!=null)
            dn_right = new AddLanesInfo(x.dn_right);
    }

    public long getId() {
        return id;
    }

    public AddLanesInfo getUp_left() {
        return up_left;
    }

    public AddLanesInfo getUp_right() {
        return up_right;
    }

    public AddLanesInfo getDn_left() {
        return dn_left;
    }

    public AddLanesInfo getDn_right() {
        return dn_right;
    }

    @Override
    public String toString() {
        return "RoadGeomInfo{" +
                "id=" + id +
                ", up_left=" + up_left +
                ", up_right=" + up_right +
                ", dn_left=" + dn_left +
                ", dn_right=" + dn_right +
                '}';
    }
}
