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
