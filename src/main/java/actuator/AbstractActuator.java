package actuator;

import control.AbstractController;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import dispatch.Pokable;
import error.OTMErrorLog;
import error.OTMException;
import output.EventsActuator;
import common.InterfaceScenarioElement;
import common.Scenario;
import common.ScenarioElementType;

public abstract class AbstractActuator implements Pokable, InterfaceScenarioElement {

    public enum Type {
        signal,
        signal_simple,
        stop,
        ramp_meter,
        fd,
        plugin,
        capacity
    }

    public long id;
    private Type type;
    public float dt;

    public AbstractController myController;
    public InterfaceActuatorTarget target;

    public EventsActuator event_listener;

    abstract public void process_controller_command(Object command, float timestamp) throws OTMException;

    ///////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public AbstractActuator(Scenario scenario, jaxb.Actuator jaxb_actuator) throws OTMException {
        this.id = jaxb_actuator.getId();
        this.type = Type.valueOf(jaxb_actuator.getType());
        this.dt = jaxb_actuator.getDt();
        if(jaxb_actuator.getActuatorTarget()!=null){
            jaxb.ActuatorTarget e = jaxb_actuator.getActuatorTarget();
            ScenarioElementType type = ScenarioElementType.valueOf(e.getType());
            this.target = (InterfaceActuatorTarget) scenario.get_element(type,e.getId());
            target.register_actuator(this);
        }
    }

    ///////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////

    @Override
    public final Long getId() {
        return id;
    }

    @Override
    public final ScenarioElementType getType() {
        return ScenarioElementType.actuator;
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        if(target==null)
            errorLog.addWarning("Actuator has no target");
    }

    @Override
    public void register_with_dispatcher(Dispatcher dispatcher) {
        dispatcher.register_event(new EventPoke(dispatcher,3,dispatcher.current_time,this));
    }

    @Override
    public OTMErrorLog to_jaxb() {
        return null;
    }

    /////////////////////////////////////////////////////////////////////
    // update
    /////////////////////////////////////////////////////////////////////

    public Type getActuatorType(){
        return type;
    }

    @Override
    public void poke(Dispatcher dispatcher, float timestamp) throws OTMException {

        // process the command
        if(myController!=null)
            process_controller_command(myController.get_command_for_actuator_id(id),timestamp);

        // wake up in dt, if dt is defined
        if(dt>0)
            dispatcher.register_event(new EventPoke(dispatcher,3,timestamp+dt,this));
    }

    /////////////////////////////////////////////////////////////////////
    // listeners
    /////////////////////////////////////////////////////////////////////

    public void set_event_listener(EventsActuator e) throws OTMException {
        if(event_listener!=null)
            throw new OTMException("multiple listeners for commodity");
        event_listener = e;
    }

}
