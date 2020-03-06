package actuator;

import common.Link;
import common.RoadConnection;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import runner.Scenario;

public class ActuatorRampMeter extends ActuatorCapacity {

    public enum Color {green,red}

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ActuatorRampMeter(Scenario scenario, jaxb.Actuator jaxb_actuator) throws OTMException {
        super(scenario,jaxb_actuator);

        // must be on a link
        if(target==null || !(target instanceof common.Link))
            return;
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
    }

    @Override
    public void process_controller_command(Object command, float timestamp) {
        if(command==null)
            return;
        Link link = (Link) target;
        Color color = (Color) command;
        float rate_vps = color==Color.red ? 0f : Float.POSITIVE_INFINITY;
        for(RoadConnection rc : link.get_roadconnections_leaving())
            rc.set_external_max_flow_vps(timestamp,rate_vps);
    }
}
