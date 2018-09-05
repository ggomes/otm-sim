package actuator;

import control.AbstractController;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import dispatch.InterfacePokable;
import error.OTMErrorLog;
import error.OTMException;
import output.OutputEventsActuator;
import runner.InterfaceScenarioElement;
import runner.Scenario;
import runner.ScenarioElementType;

public abstract class AbstractActuator implements InterfacePokable, InterfaceScenarioElement {

    public enum Type {
        signal,
        plugin
    }

    public long id;
    public Type type;
    public float dt;

    public AbstractController myController;
    public InterfaceActuatorTarget target;

    public OutputEventsActuator event_listener;

    /////////////////////////////////////////////////////////////////////
    // construction and update
    /////////////////////////////////////////////////////////////////////

    public AbstractActuator(Scenario scenario, jaxb.Actuator jaxb_actuator) throws OTMException {
        this.id = jaxb_actuator.getId();
        this.type = Type.valueOf(jaxb_actuator.getType());
        if(jaxb_actuator.getActuatorTarget()!=null){
            jaxb.ActuatorTarget e = jaxb_actuator.getActuatorTarget();
            ScenarioElementType type = ScenarioElementType.valueOf(e.getType());
            this.target = (InterfaceActuatorTarget) scenario.get_element(type,e.getId());
            target.register_actuator(this);
        }
    }

    public void validate(OTMErrorLog errorLog) {
        if(target==null)
            errorLog.addWarning("Actuator has no target");
    }

    abstract public void initialize(Scenario scenario) throws OTMException;

    /////////////////////////////////////////////////////////////////////
    // update
    /////////////////////////////////////////////////////////////////////

    abstract public void process_controller_command(Object command, Dispatcher dispatcher, float timestamp) throws OTMException;

    @Override
    public void poke(Dispatcher dispatcher, float timestamp) throws OTMException {

        // process the command
        process_controller_command(myController.get_current_command(),dispatcher,timestamp);

        // wake up in dt, if dt is defined
        if(dt>0)
            dispatcher.register_event(new EventPoke(dispatcher,1,timestamp+dt,this));
    }

    /////////////////////////////////////////////////////////////////////
    // listeners
    /////////////////////////////////////////////////////////////////////

    public void set_event_listener(OutputEventsActuator e) throws OTMException {
        if(event_listener!=null)
            throw new OTMException("multiple listeners for commodity");
        event_listener = e;
    }

    /////////////////////////////////////////////////////////////////////
    // scenario interactions
    /////////////////////////////////////////////////////////////////////

//    public void register_with_target() throws OTMException {
//        if(node==null)
//            return;
//        node.register_actuator(this);
//        for(_SignalPhaseNEMA sp : signal_phases.values())
//            sp.register_initial_events(this);
//    }


    ////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////

    @Override
    public long getId() {
        return id;
    }

    @Override
    public ScenarioElementType getScenarioElementType() {
        return ScenarioElementType.actuator;
    }


}
