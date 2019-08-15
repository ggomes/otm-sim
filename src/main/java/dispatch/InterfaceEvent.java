package dispatch;

import error.OTMException;

public interface InterfaceEvent {
    void action(boolean verbose) throws OTMException;
}
