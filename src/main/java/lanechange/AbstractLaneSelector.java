package lanechange;

import core.Link;
import core.Scenario;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import dispatch.Pokable;
import error.OTMException;

public abstract class AbstractLaneSelector implements Pokable {

    protected Float dt;
    protected Link  link;
    protected abstract void update();  // write state2lanechangeprob for all lanegroups in link

    public AbstractLaneSelector(Link link, Float dt){
        this.link = link;
        this.dt = dt;
    }

    public void initialize(Scenario scenario, float start_time) throws OTMException {
        poke(scenario.dispatcher,start_time);
    }

    @Override
    public void poke(Dispatcher dispatcher, float timestamp) throws OTMException {
        update();
        if(dt!=null)
            dispatcher.register_event(new EventPoke(dispatcher,5,timestamp+dt,this));
    }

}
