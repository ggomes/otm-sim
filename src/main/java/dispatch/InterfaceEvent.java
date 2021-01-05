package dispatch;

import error.OTMException;

public interface InterfaceEvent {
    void action() throws OTMException;
}
