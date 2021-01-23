package sensor;

import actuator.InterfaceTarget;
import core.*;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import dispatch.Pokable;
import error.OTMErrorLog;
import error.OTMException;
import utils.OTMUtils;

import java.util.HashSet;

public abstract class AbstractSensor implements Pokable, InterfaceScenarioElement {

    public enum Type {
        fixed
    }

    public Long id;
    public Type type;
    public Float dt;
    public double dt_inv;

    public InterfaceTarget target;

    /////////////////////////////////////////////////////////////////////
    // construction
    /////////////////////////////////////////////////////////////////////

    public AbstractSensor(Long id, Type type, float dt) {
        this.id = id;
        this.type = type;
        this.dt = dt;
    }

    public AbstractSensor(jaxb.Sensor jaxb_sensor) {
        this(jaxb_sensor.getId(),Type.valueOf(jaxb_sensor.getType()),jaxb_sensor.getDt());
    }

    ////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////

    public void initialize(Scenario scenario) throws OTMException {

        if(dt==null || dt<=0){
            AbstractModel model = this.target.get_model();
            if(model==null) {
                dt = null;
            }
            else {
                if (model instanceof AbstractFluidModel)
                    dt = ((AbstractFluidModel) model).dt_sec;
                if (model instanceof AbstractVehicleModel)
                    dt = null;
            }
        }

        dt_inv = dt==null ? null : 3600d/dt;

        Dispatcher dispatcher = scenario.dispatcher;
        dispatcher.register_event(new EventPoke(dispatcher,10,dispatcher.current_time,this));
    }

    public void validate_post_init(OTMErrorLog errorLog){
        if(target==null)
            errorLog.addError("Sensor has null target.");
    }

    @Override
    public final ScenarioElementType getSEType() {
        return ScenarioElementType.sensor;
    }

    @Override
    public final Long getId() {
        return id;
    }

    @Override
    public OTMErrorLog to_jaxb() {
        return null;
    }

    /////////////////////////////////////////////////////////////////////
    // update
    /////////////////////////////////////////////////////////////////////

    abstract public void take_measurement(Dispatcher dispatcher, float timestamp);

    @Override
    public void poke(Dispatcher dispatcher, float timestamp) {
        take_measurement(dispatcher,timestamp);

        // write to output
//        if(event_output!=null)
//            event_output.write(timestamp,new EventWrapperSensor(measurement));

        // wake up in dt, if dt is defined
        if(dt>0)
            dispatcher.register_event(new EventPoke(dispatcher,1,timestamp+dt,this));
    }

    /////////////////////////////////////////////////////////////////////
    // InterfaceEventWriter
    /////////////////////////////////////////////////////////////////////

//    @Override
//    public void set_event_output(AbstractOutputEvent e) throws OTMException {
//        if(event_output !=null)
//            throw new OTMException("multiple listeners for sensor");
//        if(!(e instanceof OutputSensor))
//            throw new OTMException("Wrong type of listener");
//        event_output = (OutputSensor) e;
//    }
}
