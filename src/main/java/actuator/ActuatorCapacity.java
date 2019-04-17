package actuator;

import common.Link;
import common.RoadConnection;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Actuator;
import runner.Scenario;

public class ActuatorCapacity extends AbstractActuator  {

    public float rate_vps;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ActuatorCapacity(Scenario scenario, Actuator jaxb_actuator) throws OTMException {
        super(scenario, jaxb_actuator);
        rate_vps = 0f;
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        rate_vps = 0f;
    }

    @Override
    public void process_controller_command(Object command, Dispatcher dispatcher, float timestamp) throws OTMException {
        if(command==null)
            return;
        Link link = (Link) target;
        for(RoadConnection rc : link.get_roadconnections_leaving())
            rc.set_external_max_flow_vps(timestamp,rate_vps);
    }

    ///////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////

    public void set_rate_vph(float rate_vph){
        this.rate_vps = rate_vph / 3600f;
    }

}
