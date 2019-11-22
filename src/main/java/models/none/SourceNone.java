package models.none;

import commodity.Commodity;
import commodity.Path;
import common.Link;
import profiles.DemandProfile;

public class SourceNone extends common.AbstractSource {
	public SourceNone(Link link, DemandProfile profile, Commodity commodity, Path path) {
		super(link, profile, commodity, path);
	}
}
