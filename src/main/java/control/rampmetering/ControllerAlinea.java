package control.rampmetering;

import actuator.AbstractActuator;
import actuator.ActuatorMeter;
import common.LaneGroupSet;
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
import java.util.stream.Collectors;

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

        assert(actuators.size()==1);

        params = new HashMap<>();
        for(AbstractActuator abs_act : actuators.values()){
            ActuatorMeter act = (ActuatorMeter) abs_act;
            AlineaParams param = new AlineaParams();

            FixedSensor ml_sensor = (FixedSensor) sensors.iterator().next();
            Link ml_link = ml_sensor.get_link();

            Roadparam p = ml_link.road_param;
            param.gain_per_sec = p.getSpeed() * 1000f / 3600f / ml_link.length ; // [kph]*1000/3600/[m] -> [mps]
            float critical_density_vpkpl = p.getCapacity() / p.getSpeed();  // vpkpl
            param.ref_density_veh = critical_density_vpkpl * ml_link.full_lanes * ml_link.length / 1000f;

            param.max_rate_vps = Math.min(cntrl_max_rate_vpspl*act.total_lanes,act.max_rate_vps);
            param.min_rate_vps = Math.max(cntrl_min_rate_vpspl*act.total_lanes,act.min_rate_vps);
            param.ref_link = ml_link;

            // all lanegroups in the actuator must be in the same link
            LaneGroupSet lgs = (LaneGroupSet)act.target;
            Set<Link> ors = lgs.lgs.stream().map(lg->lg.link).collect(Collectors.toSet());

            if(ors.size()!=1)
                throw new OTMException("All lanegroups in any single actuator used by an Alinea controller must belong to the same link.");

            Link orlink = ors.iterator().next();
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
            float rate_vps  = compute_alinea_rate_vps(act,p);
            if(has_queue_control){
                Link or = ((LaneGroupSet) act.target).lgs.iterator().next().link;
                if(or.get_veh()>= p.queue_threshold)
                    rate_vps = p.max_rate_vps;
            }
            command.put(act.id, new CommandNumber( rate_vps ) );
        }
    }

    private float compute_alinea_rate_vps(ActuatorMeter act,AlineaParams p){
        float density_veh = (float) p.ref_link.get_veh();
        float previous_rate_vps = ((CommandNumber) command.get(act.id)).value;
        float alinea_rate_vps = previous_rate_vps +  p.gain_per_sec * (p.ref_density_veh - density_veh);
        return Math.min( Math.max(alinea_rate_vps,p.min_rate_vps) , p.max_rate_vps );
    }

    public class AlineaParams {
        float gain_per_sec;
        float ref_density_veh;
        float min_rate_vps;
        float max_rate_vps;
        Link ref_link;
        float queue_threshold;
    }
}
