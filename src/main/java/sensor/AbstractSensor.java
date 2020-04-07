package sensor;

import dispatch.Dispatcher;
import dispatch.EventPoke;
import dispatch.Pokable;
import error.OTMErrorLog;
import error.OTMException;
import output.EventsSensor;
import common.InterfaceScenarioElement;
import common.Scenario;
import common.ScenarioElementType;

public abstract class AbstractSensor implements Pokable, InterfaceScenarioElement {

    public enum Type {
        fixed,
        commodity,
        plugin
    }

    public long id;
    public Type type;
    public float dt;
    public double dt_inv;

    public Object target;

    public EventsSensor event_listener;

    /////////////////////////////////////////////////////////////////////
    // construction
    /////////////////////////////////////////////////////////////////////

    public AbstractSensor(Scenario scenario, jaxb.Sensor jaxb_sensor) {
        this.id = jaxb_sensor.getId();
        this.type = Type.valueOf(jaxb_sensor.getType());
        this.dt = jaxb_sensor.getDt();
        this.dt_inv = 3600d/dt;
    }

    ////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////

    @Override
    public final ScenarioElementType getType() {
        return ScenarioElementType.sensor;
    }

    @Override
    public final Long getId() {
        return id;
    }

    @Override
    public void register_with_dispatcher(Dispatcher dispatcher){
        dispatcher.register_event(new EventPoke(dispatcher,1,dispatcher.current_time,this));
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

        // wake up in dt, if dt is defined
        if(dt>0)
            dispatcher.register_event(new EventPoke(dispatcher,1,timestamp+dt,this));
    }

    /////////////////////////////////////////////////////////////////////
    // listeners
    /////////////////////////////////////////////////////////////////////

    public void set_event_listener(EventsSensor e) throws OTMException {
        if(event_listener!=null)
            throw new OTMException("multiple listeners for commodity");
        event_listener = e;
    }
}
