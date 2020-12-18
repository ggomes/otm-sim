package traveltime;

import core.Link;
import dispatch.Dispatcher;
import dispatch.EventComputeTravelTime;
import error.OTMException;
import output.OutputPathTravelTime;
import core.Scenario;

import java.util.*;

public class LinkTravelTimeManager {

    public Scenario scenario;
    public float dt;
    public Set<Link> links;

    public LinkTravelTimeManager(Scenario scenario){
        this.scenario = scenario;
        this.dt = Float.NaN;
        this.links = new HashSet<>();
    }

    public void add_path_travel_time_writer(OutputPathTravelTime path_tt_writer) throws OTMException {

        // check dt
        if (Float.isNaN(dt))
            this.dt = path_tt_writer.outDt;
        else if (this.dt!=path_tt_writer.outDt)
                throw new OTMException("All path travel time requests must have the same dt.");

        // add all links to set
        links.addAll(path_tt_writer.path.get_ordered_links());
    }

    public void initialize(Dispatcher dispatcher){

        // create link travel timers
        for(Link link : links)
            link.link_tt = new LinkTravelTimer(link,dt);

        dispatcher.register_event(new EventComputeTravelTime(dispatcher,dispatcher.current_time,this));
    }

    // called by EventComputeTravelTime
    public void run(float now){

        // update link travel times
        links.forEach(link->link.link_tt.update_travel_time());

        // set new event
        scenario.dispatcher.register_event(new EventComputeTravelTime(scenario.dispatcher,now+dt,this));
    }

}
