package lanechange;

import core.AbstractLaneGroup;
import core.Link;
import core.State;
import models.Maneuver;

import java.util.List;
import java.util.Map;

public class KeepLaneSelector extends AbstractLaneSelector {

    public KeepLaneSelector(Link link) {
        super(link, null);
    }

    @Override
    protected void update() {
        for(AbstractLaneGroup lg : link.get_lgs()){
            for(State state : lg.get_link().states ){
                Map<Maneuver,Double> x = lg.get_maneuvprob_for_state(state);

                // non discretionary
                if(x.size()==1){
                    x.put(x.keySet().iterator().next(),1d);
                    continue;
                }

                // discretionary stay
                if(x.keySet().contains(Maneuver.stay))
                    for(Maneuver m : x.keySet())
                        x.put(m, m==Maneuver.stay?1d:0d);
                else {
                    if(x.size()==2)
                        for(Maneuver m : x.keySet())
                            x.put(m, 0.5d);
                    else
                        x.put(x.keySet().iterator().next(),1d);
                }


            }
        }
    }

}
