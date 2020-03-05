package dispatch;

import error.OTMException;

public interface Pokable {

    void poke(Dispatcher dispatcher, float timestamp) throws OTMException;

}
