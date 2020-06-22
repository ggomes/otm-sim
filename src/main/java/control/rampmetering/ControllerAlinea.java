package control.rampmetering;

import actuator.AbstractActuator;
import actuator.ActuatorMeter;
import common.AbstractLaneGroup;
import common.Node;
import control.AbstractController;
import control.command.CommandNumber;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Controller;
import common.Link;
import jaxb.Roadparam;
import common.Scenario;
import sensor.FixedSensor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class ControllerAlinea extends AbstractController {

    public Map<Long,AlineaParams> params;
    public float cntrl_max_rate_vpspl;
    public float cntrl_min_rate_vpspl;

    // queue control
    private boolean has_queue_control;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ControllerAlinea(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);

        cntrl_max_rate_vpspl = Float.POSITIVE_INFINITY;
        cntrl_min_rate_vpspl = Float.NEGATIVE_INFINITY;
        this.has_queue_control = false;
        if(jaxb_controller.getParameters()!=null){
            for(jaxb.Parameter p : jaxb_controller.getParameters().getParameter()){
                if(p.getName().compareTo("max_rate_vphpl")==0)
                    cntrl_max_rate_vpspl = Float.parseFloat(p.getValue())/3600f;
                if(p.getName().compareTo("min_rate_vphpl")==0)
                    cntrl_min_rate_vpspl = Float.parseFloat(p.getValue())/3600f;
                if(p.getName().compareTo("queue_control")==0)
                    has_queue_control = Boolean.parseBoolean(p.getValue());
            }
        }
    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);

        params = new HashMap<>();
        for(AbstractActuator abs_act : actuators.values()){
            ActuatorMeter act = (ActuatorMeter) abs_act;
            AlineaParams param = new AlineaParams();

            FixedSensor ml_sensor = (FixedSensor) sensors.iterator().next();
            Link ml_link = ml_sensor.get_link();

            Roadparam p = ml_link.road_param;
            param.gain_per_sec = p.getSpeed() * 1000f / 3600f / ml_link.length ; // [kph]*1000/3600/[m] -> [mps]
            float critical_density_vpkpl = p.getCapacity() / p.getSpeed();  // vpkpl
            param.target_density_veh = critical_density_vpkpl * ml_link.full_lanes * ml_link.length / 1000f;

            param.max_rate_vps = Math.min(cntrl_max_rate_vpspl*act.total_lanes,act.max_rate_vps);
            param.min_rate_vps = Math.max(cntrl_min_rate_vpspl*act.total_lanes,act.min_rate_vps);
            param.target_link = ml_link;

            Link orlink = ((AbstractLaneGroup)act.target).link;
            param.queue_threshold = orlink.road_param.getJamDensity() * orlink.full_lanes * orlink.length / 1000;

            params.put(abs_act.id , param);

            command.put(act.id,
                    new CommandNumber(Float.isInfinite(act.max_rate_vps) ? (float) ml_link.full_lanes*900f/3600f : act.max_rate_vps)
            );
        }
    }

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {
        for(AbstractActuator abs_act : this.actuators.values()){
            ActuatorMeter act = (ActuatorMeter) abs_act;
            AlineaParams p = params.get(act.id);
            double queue = has_queue_control ? ((AbstractLaneGroup) act.target).link.get_veh() : 0f;
            command.put(act.id,
                    new CommandNumber( queue<p.queue_threshold ? compute_alinea_rate_vps(act,p) : p.max_rate_vps ) );
        }
    }

    private float compute_alinea_rate_vps(ActuatorMeter act,AlineaParams p){
        float density_veh = (float) p.target_link.get_veh();
        float previous_rate_vps = ((CommandNumber) command.get(act.id)).value;
        float alinea_rate_vps = previous_rate_vps +  p.gain_per_sec * (p.target_density_veh - density_veh);

        System.out.println(alinea_rate_vps*3600f);

        return Math.min( Math.max(alinea_rate_vps,p.min_rate_vps) , p.max_rate_vps );
    }

    public class AlineaParams {
        float gain_per_sec;
        float target_density_veh;
        float min_rate_vps;
        float max_rate_vps;
        Link target_link;
        float queue_threshold;
    }
}
