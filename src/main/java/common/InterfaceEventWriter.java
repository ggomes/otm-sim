package common;

import error.OTMException;
import output.AbstractOutputEvent;

public interface InterfaceEventWriter {
    void set_event_output(AbstractOutputEvent e) throws OTMException;
}
