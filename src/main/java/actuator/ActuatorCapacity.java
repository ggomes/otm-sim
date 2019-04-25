package actuator;

import common.Link;
import common.RoadConnection;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Actuator;
import runner.Scenario;

public class ActuatorCapacity extends AbstractActuator  {

//    public float rate_vps;
    public float max_rate_vps;
    public float min_rate_vps;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ActuatorCapacity(Scenario scenario, Actuator jact) throws OTMException {
        super(scenario, jact);
//        rate_vps = 0f;

        // interpret jact.getMaxValue and jact.getMinValue in vphpl
        int num_lanes = ((Link)target).full_lanes;
        max_rate_vps = jact.getMaxValue()>=0f ? jact.getMaxValue()*num_lanes/3600f : Float.POSITIVE_INFINITY;
        min_rate_vps = jact.getMinValue()>=0f ? jact.getMinValue()*num_lanes/3600f : 0f;
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
//        rate_vps = 0f;
    }

    @Override
    public void process_controller_command(Object command, Dispatcher dispatcher, float timestamp) throws OTMException {
        if(command==null)
            return;
        Link link = (Link) target;
        float rate_vps = (float) command;
        for(RoadConnection rc : link.get_roadconnections_leaving())
            rc.set_external_max_flow_vps(timestamp,rate_vps);
    }

    ///////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////

    public void set_rate_vph(float rate_vph){
        float rate_vps = rate_vph / 3600f;

        Link link = (Link) target;
        for(RoadConnection rc : link.get_roadconnections_leaving())
            rc.set_external_max_flow_vps(-1f,rate_vps);
    }

}
