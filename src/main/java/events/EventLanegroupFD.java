package events;

import core.AbstractLaneGroup;
import core.Scenario;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Roadparam;

import java.util.HashMap;
import java.util.Map;

public class EventLanegroupFD extends AbstractLanegroupEvent {

    jaxb.Roadparam fd;
    Map<Long, Roadparam> oldfds;

    public EventLanegroupFD(long id, EventType type, float timestamp) {
        super(id, type, timestamp);
    }

    public EventLanegroupFD(Scenario scenario, jaxb.Event jev) throws OTMException {
        super(scenario,jev);

        this.fd = new jaxb.Roadparam();
        if(jev.getParameters()!=null)
            for(jaxb.Parameter p : jev.getParameters().getParameter()) {
                if (p.getName().equals("capacity"))
                    fd.setCapacity(Float.parseFloat(p.getValue()));
                if (p.getName().equals("jam_density"))
                    fd.setJamDensity(Float.parseFloat(p.getValue()));
                if (p.getName().equals("speed"))
                    fd.setSpeed(Float.parseFloat(p.getValue()));
            }

    }

    @Override
    public void validate_pre_init(OTMErrorLog errorLog) {
        super.validate_pre_init(errorLog);
        if(fd.getCapacity()<0)
            errorLog.addError("Negative capacity in event");
        if(fd.getJamDensity()<0)
            errorLog.addError("Negative jam density in event");
        if(fd.getSpeed()<0)
            errorLog.addError("Negative speed in event");
    }

    @Override
    public void validate_post_init(OTMErrorLog errorLog) {

    }

    @Override
    public void action() throws OTMException {
        System.out.println(String.format("%.2f\t%s",timestamp,getClass().getName()));

        oldfds = new HashMap<>();
        for(AbstractLaneGroup lg : lanegroups){
            oldfds.put(lg.getId(),lg.get_road_params());
            lg.set_road_params(fd);
        }
    }
}
