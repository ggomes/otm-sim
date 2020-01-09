package models.none;

import commodity.Commodity;
import commodity.Path;
import common.Link;
import profiles.DemandProfile;

public class NoneSource extends common.AbstractSource {
	public NoneSource(Link link, DemandProfile profile, Commodity commodity, Path path) {
		super(link, profile, commodity, path);
	}
}
