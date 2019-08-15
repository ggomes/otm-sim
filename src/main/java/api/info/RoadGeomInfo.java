package api.info;

import geometry.RoadGeometry;

public class RoadGeomInfo {

    /** Integer id for this road geometry. */
    public long id;

    /** Upstream inside side additional lanes. */
    public AddLanesInfo up_in;

    /** Upstream outside side additional lanes. */
    public AddLanesInfo up_out;

    /** Downstream inside side additional lanes. */
    public AddLanesInfo dn_in;

    /** Downstream outside side additional lanes. */
    public AddLanesInfo dn_out;

    public RoadGeomInfo(RoadGeometry x){

        this.id = x.id;

        if(x.up_in !=null)
            up_in = new AddLanesInfo(x.up_in);

        if(x.up_out !=null)
            up_out = new AddLanesInfo(x.up_out);

        if(x.dn_in !=null)
            dn_in = new AddLanesInfo(x.dn_in);

        if(x.dn_out !=null)
            dn_out = new AddLanesInfo(x.dn_out);
    }

    public long getId() {
        return id;
    }

    public AddLanesInfo getUp_in() {
        return up_in;
    }

    public AddLanesInfo getUp_out() {
        return up_out;
    }

    public AddLanesInfo getDn_in() {
        return dn_in;
    }

    public AddLanesInfo getDn_out() {
        return dn_out;
    }

    @Override
    public String toString() {
        return "RoadGeomInfo{" +
                "id=" + id +
                ", up_in=" + up_in +
                ", up_out=" + up_out +
                ", dn_in=" + dn_in +
                ", dn_out=" + dn_out +
                '}';
    }
}
