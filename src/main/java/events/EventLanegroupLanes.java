package events;

import core.AbstractLaneGroup;
import core.Scenario;
import error.OTMException;

public class EventLanegroupLanes extends AbstractLanegroupEvent {

    public int dlanes; // number of lanes added or subtracted by this event

    public EventLanegroupLanes(long id, EventType type, float timestamp) {
        super(id, type, timestamp);
    }

    public EventLanegroupLanes(Scenario scenario, jaxb.Event jev) throws OTMException {
        super(scenario,jev);

        this.dlanes = 0;
        if(jev.getParameters()!=null)
            for(jaxb.Parameter p : jev.getParameters().getParameter())
                if(p.getName().equals("dlanes"))
                    this.dlanes = Integer.parseInt(p.getValue());

    }

    @Override
    public void action() throws OTMException {
        System.out.println(String.format("%.2f\t%s",timestamp,getClass().getName()));

        for(AbstractLaneGroup lg : lanegroups){

            jaxb.Roadparam rp = lg.get_road_params();
            int oldlanes = lg.get_num_lanes();
            int newlanes =oldlanes+dlanes;

            if(newlanes<0)
                throw new OTMException("Event sets number of lanes to a negative number.");

            rp.setCapacity( rp.getCapacity()*newlanes/oldlanes );
            lg.set_road_params(rp);
        }

    }
}
