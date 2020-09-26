package control.rampmetering;

import actuator.AbstractActuator;
import actuator.ActuatorMeter;
import common.AbstractLaneGroup;
import common.LaneGroupSet;
import common.Link;
import common.Scenario;
import control.AbstractController;
import control.command.CommandNumber;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Controller;
import models.AbstractModel;
import models.fluid.FluidLaneGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractControllerRampMetering extends AbstractController {

    protected boolean has_queue_control;
    protected float max_rate_vpspl;
    protected float min_rate_vpspl;
    protected float override_threshold;

    protected Map<Long,MeterParams> meterparams;

    protected abstract float compute_nooverride_rate_vps(ActuatorMeter act,float timestamp);

    public AbstractControllerRampMetering(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);

        has_queue_control = false;
        min_rate_vpspl = Float.NEGATIVE_INFINITY;
        max_rate_vpspl = Float.POSITIVE_INFINITY;
        if(jaxb_controller.getParameters()!=null){
            for(jaxb.Parameter p : jaxb_controller.getParameters().getParameter()){
                if(p.getName().compareTo("max_rate_vphpl")==0)
                    max_rate_vpspl = Float.parseFloat(p.getValue())/3600f;
                if(p.getName().compareTo("min_rate_vphpl")==0)
                    min_rate_vpspl = Float.parseFloat(p.getValue())/3600f;
                if(p.getName().compareTo("queue_control")==0)
                    has_queue_control = Boolean.parseBoolean(p.getValue());
                if(p.getName().compareTo("override_threshold")==0)
                    override_threshold = Float.parseFloat(p.getValue());
            }
        }

    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
        meterparams = new HashMap<>();
        for (AbstractActuator abs_act : actuators.values()) {
            ActuatorMeter act = (ActuatorMeter) abs_act;
            float min_rate_vps = min_rate_vpspl * act.total_lanes;
            float max_rate_vps = max_rate_vpspl * act.total_lanes;
            float thres = Float.NaN;
            if(has_queue_control) {
                FluidLaneGroup lg = (FluidLaneGroup) ((LaneGroupSet) act.target).lgs.iterator().next();
                thres = (float) (override_threshold * lg.jam_density_veh_per_cell);
            }
            meterparams.put(abs_act.id,new MeterParams(thres,min_rate_vps,max_rate_vps));
        }

    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);
        if(has_queue_control){
            for(AbstractActuator abs_act : actuators.values()) {
                Set<AbstractLaneGroup> lgs =  ((LaneGroupSet) abs_act.target).lgs;
                if(lgs.size()!=1)
                    errorLog.addError("Queue overide actuators must be associated with a single lane group");
                Link or = lgs.iterator().next().link;
                if(or.is_source)
                    errorLog.addError("Queue override does not work when the onramp is a source link.");
                if(!or.model.type.equals(AbstractModel.Type.Fluid))
                    errorLog.addError("Queue override is only implemented for fluid models.");
            }
        }
    }

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {
        for(AbstractActuator abs_act : actuators.values()) {
            MeterParams params = meterparams.get(abs_act.id);
            float rate_vps = compute_nooverride_rate_vps((ActuatorMeter) abs_act,dispatcher.current_time);
            if(has_queue_control){
                FluidLaneGroup orlg = (FluidLaneGroup) ((LaneGroupSet) abs_act.target).lgs.iterator().next();
                double veh = orlg.cells.get(0).get_vehicles();
                if(veh >= params.queue_threshold)
                    rate_vps = params.max_rate_vps;
            }
            if(rate_vps<params.min_rate_vps)
                rate_vps = params.min_rate_vps;
            if(rate_vps>params.max_rate_vps)
                rate_vps = params.max_rate_vps;
            this.command.put(abs_act.id, new CommandNumber(rate_vps));
        }
    }

    public class MeterParams {
        float queue_threshold;
        float max_rate_vps;
        float min_rate_vps;
        public MeterParams(float queue_threshold,float min_rate_vps,float max_rate_vps) {
            this.queue_threshold = queue_threshold;
            this.min_rate_vps = min_rate_vps;
            this.max_rate_vps = max_rate_vps;
        }
    }

}
