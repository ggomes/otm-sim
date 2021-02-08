package events;

import core.AbstractLaneGroup;
import core.Scenario;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Roadparam;

import java.util.HashMap;
import java.util.Map;

public class EventLanegroupFD extends AbstractLanegroupEvent {

    jaxb.Roadparam fd_mult;
    Map<Long, Roadparam> oldfds;

    public EventLanegroupFD(long id, EventType type, float timestamp, String name) {
        super(id, type, timestamp,name);
    }

    public EventLanegroupFD(Scenario scenario, jaxb.Event jev) throws OTMException {
        super(scenario,jev);

        this.fd_mult = null;
        if(jev.getParameters()!=null) {
            fd_mult = new jaxb.Roadparam();
            fd_mult.setCapacity(1f);
            fd_mult.setJamDensity(1f);
            fd_mult.setSpeed(1f);
            for (jaxb.Parameter p : jev.getParameters().getParameter()) {
                if (p.getName().equals("capacity"))
                    fd_mult.setCapacity(Float.parseFloat(p.getValue()));
                if (p.getName().equals("jam_density"))
                    fd_mult.setJamDensity(Float.parseFloat(p.getValue()));
                if (p.getName().equals("speed"))
                    fd_mult.setSpeed(Float.parseFloat(p.getValue()));
            }
        }

    }

    @Override
    public void validate_pre_init(OTMErrorLog errorLog) {
        super.validate_pre_init(errorLog);
        if(fd_mult.getCapacity()<0)
            errorLog.addError("Negative capacity multiplier in event");
        if(fd_mult.getJamDensity()<0)
            errorLog.addError("Negative jam density multiplier  in event");
        if(fd_mult.getSpeed()<0)
            errorLog.addError("Negative speed multiplier  in event");
    }

    @Override
    public void validate_post_init(OTMErrorLog errorLog) {

    }

    @Override
    public void action() throws OTMException {

        if(fd_mult==null)
            for(AbstractLaneGroup lg : lanegroups)
                lg.reset_road_params();
        else
            for(AbstractLaneGroup lg : lanegroups) {
                jaxb.Roadparam rp = lg.get_road_params();
                jaxb.Roadparam newrp = new jaxb.Roadparam();
                newrp.setCapacity(rp.getCapacity()*fd_mult.getCapacity());
                newrp.setJamDensity(rp.getJamDensity()*fd_mult.getJamDensity());
                newrp.setSpeed(rp.getSpeed()*fd_mult.getSpeed());

                lg.set_road_params(newrp);
            }
    }
}
