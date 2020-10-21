package models.none;

import commodity.Commodity;
import commodity.Path;
import common.AbstractDemandGenerator;
import common.Link;
import profiles.Profile1D;

public class NoneDemandGenerator extends AbstractDemandGenerator {
	public NoneDemandGenerator(Link link, Profile1D profile, Commodity commodity, Path path) {
		super(link, profile, commodity, path);
	}
}
