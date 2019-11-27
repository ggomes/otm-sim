package models;

import commodity.Commodity;
import commodity.Path;
import common.Link;
import profiles.DemandProfile;

public class BaseSourceNone extends common.AbstractSource {
	public BaseSourceNone(Link link, DemandProfile profile, Commodity commodity, Path path) {
		super(link, profile, commodity, path);
	}
}
