package actuator;

import common.Link;
import error.OTMException;
import common.Scenario;

public class ActuatorGreenRed extends AbstractActuator {

    public enum Color {green,red}

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ActuatorGreenRed(Scenario scenario, jaxb.Actuator jaxb_actuator) throws OTMException {
        super(scenario,jaxb_actuator);

        // must be on a link
        if(target==null || !(target instanceof common.Link))
            return;
    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void initialize(Scenario scenario) throws OTMException {

    }

    ///////////////////////////////////////////////////
    // AbstractActuator
    ///////////////////////////////////////////////////

    @Override
    public void process_controller_command(Object command, float timestamp) {
        if(command==null)
            return;
        Link link = (Link) target;
        Color color = (Color) command;
        float rate_vps = color==Color.red ? 0f : Float.POSITIVE_INFINITY;
//        for(RoadConnection rc : link.outlink2roadconnection.values())
//            rc.set_external_max_flow_vps(timestamp,rate_vps);
    }
}
