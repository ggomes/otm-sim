package control;

import actuator.AbstractActuator;
import actuator.ActuatorCapacity;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Controller;
import common.Link;
import jaxb.Roadparam;
import runner.Scenario;

import java.util.HashMap;
import java.util.Map;

public class ControllerAlinea extends AbstractController  {

    public Map<Long,AlineaParams> params;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ControllerAlinea(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);
    }

    @Override
    public void initialize(Scenario scenario, float now) throws OTMException {
        params = new HashMap<>();
        for(AbstractActuator abs_act : actuators.values()){
            ActuatorCapacity act = (ActuatorCapacity) abs_act;
            AlineaParams param = new AlineaParams();

            Link link = (Link) abs_act.target;

            Roadparam p = link.road_param;
            param.gain_per_sec = p.getSpeed() * 1000d / 3600d / link.length ; // [kph]*1000/3600/[m]

            double critical_density_vpkpl = p.getCapacity() / p.getSpeed();  // vpkpl
            param.target_density_veh = critical_density_vpkpl * link.full_lanes * link.length / 1000f;

            param.max_rate_vps = act.max_rate_vps;

            if(Float.isInfinite(act.max_rate_vps))
                param.previous_rate_vps = link.full_lanes*900f/3600f;
            else
                param.previous_rate_vps = act.max_rate_vps;

            params.put(abs_act.id , param);
        }
    }

    @Override
    public void update_controller(Dispatcher dispatcher, float timestamp) throws OTMException {
        for(AbstractActuator abs_act : this.actuators.values()){
            ActuatorCapacity act = (ActuatorCapacity) abs_act;
            AlineaParams p = params.get(abs_act.id);
            double density_veh = ((Link) act.target).get_veh();
            double rate_vps = p.previous_rate_vps +  p.gain_per_sec * (p.target_density_veh - density_veh);
            if(rate_vps < 0f)
                rate_vps = 0f;
            else if(rate_vps > p.max_rate_vps)
                rate_vps = p.max_rate_vps;
            act.rate_vps = (float) rate_vps;
            p.previous_rate_vps = rate_vps;
        }
    }

    public class AlineaParams {
        double gain_per_sec;
        double target_density_veh;
        double previous_rate_vps;
        double max_rate_vps;
    }
}
