package output;

import error.OTMException;

public interface InterfacePlottable {
    void plot(String filename) throws OTMException;
    String get_yaxis_label();
}
