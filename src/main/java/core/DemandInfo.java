package core;

import profiles.Profile1D;

public class DemandInfo {
    long commid;
    Long pathid;
    Profile1D profile;

    public DemandInfo(long commid, Long pathid, Profile1D profile) {
        this.commid = commid;
        this.pathid = pathid;
        this.profile = profile;
    }
}
