package events;

import core.AbstractLaneGroup;
import core.Scenario;
import error.OTMException;

public class EventLanegroupLanes extends AbstractLanegroupEvent {

    public Integer dlanes; // number of lanes added or subtracted by this event

    public EventLanegroupLanes(long id, EventType type, float timestamp,String name) {
        super(id, type, timestamp,name);
    }

    public EventLanegroupLanes(Scenario scenario, jaxb.Event jev) throws OTMException {
        super(scenario,jev);

        this.dlanes = null;
        if(jev.getParameters()!=null)
            for(jaxb.Parameter p : jev.getParameters().getParameter())
                if(p.getName().equals("dlanes"))
                    this.dlanes = Integer.parseInt(p.getValue());
    }

    @Override
    public void action() throws OTMException {
        if(dlanes==null) {
            for (AbstractLaneGroup lg : lanegroups) {
                if(dispatcher.lg2deltalanes.containsKey(lg.getId())) {
                    // original lanes = current - delta
                    lg.set_lanes(lg.get_num_lanes() - dispatcher.lg2deltalanes.get(lg.getId()));
                    dispatcher.lg2deltalanes.put(lg.getId(), 0);
                }
            }
        }

        else{
            for(AbstractLaneGroup lg : lanegroups){
                int current_delta = dispatcher.lg2deltalanes.containsKey(lg.getId()) ?
                        dispatcher.lg2deltalanes.get(lg.getId()) : 0;
                dispatcher.lg2deltalanes.put(lg.getId(),current_delta+dlanes);
                lg.set_lanes(lg.get_num_lanes()+dlanes);
            }
        }

    }
}
