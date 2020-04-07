package control;

import actuator.AbstractActuator;
import actuator.ActuatorCapacity;
import common.Node;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Controller;
import common.Link;
import jaxb.Roadparam;
import common.Scenario;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class ControllerAlinea extends AbstractController  {

    public Map<Long,AlineaParams> params;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ControllerAlinea(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);
    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        params = new HashMap<>();
        for(AbstractActuator abs_act : actuators.values()){
            ActuatorCapacity act = (ActuatorCapacity) abs_act;
            AlineaParams param = new AlineaParams();

            Link onramp_link = (Link) abs_act.target;

            Node end_node = onramp_link.end_node;
            Set<Link> ml_links = end_node.in_links.values().stream()
                    .filter(link->link!=onramp_link).collect(toSet());

            if(ml_links.size()!=1)
                throw new OTMException("ml_links.size()!=1");

            Link ml_link = ml_links.iterator().next();

            Roadparam p = ml_link.road_param;
            param.gain_per_sec = p.getSpeed() * 1000f / 3600f / ml_link.length ; // [kph]*1000/3600/[m]
            float critical_density_vpkpl = p.getCapacity() / p.getSpeed();  // vpkpl
            param.target_density_veh = critical_density_vpkpl * ml_link.full_lanes * ml_link.length / 1000f;
            param.max_rate_vps = act.max_rate_vps;
            param.target_link = ml_link;
            params.put(abs_act.id , param);

            command.put(act.id,Float.isInfinite(act.max_rate_vps) ? (float) ml_link.full_lanes*900f/3600f : act.max_rate_vps);
        }
    }

    @Override
    public void update_command(Dispatcher dispatcher, float timestamp) throws OTMException {
        for(AbstractActuator abs_act : this.actuators.values()){
            ActuatorCapacity act = (ActuatorCapacity) abs_act;
            AlineaParams p = params.get(abs_act.id);
            float density_veh = (float) p.target_link.get_veh();
            float previous_rate_vps = (float) command.get(act.id);
            float rate_vps = previous_rate_vps +  p.gain_per_sec * (p.target_density_veh - density_veh);

            if(rate_vps < 0f)
                rate_vps = 0f;
            else if(rate_vps > p.max_rate_vps)
                rate_vps = p.max_rate_vps;

            command.put(act.id,rate_vps);

        }
    }

    public class AlineaParams {
        float gain_per_sec;
        float target_density_veh;
        float max_rate_vps;
        Link target_link;
    }
}
