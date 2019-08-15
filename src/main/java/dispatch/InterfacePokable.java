package dispatch;

import error.OTMException;

public interface InterfacePokable {

    void poke(Dispatcher dispatcher, float timestamp) throws OTMException;

}
