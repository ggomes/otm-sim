package actuator;

import common.*;
import control.AbstractController;
import control.command.InterfaceCommand;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import dispatch.Pokable;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Actuator;
import utils.OTMUtils;

import java.util.Set;

public abstract class AbstractActuator implements Pokable, InterfaceScenarioElement {

    public enum Type {
        lg_restrict,
        lg_speed,
        signal,
        meter,
        stop,
        split
    }

    public long id;
    public abstract Type getType();
    public float dt;

    public AbstractController myController;
    public InterfaceActuatorTarget target;

    abstract public void process_controller_command(InterfaceCommand command, float timestamp) throws OTMException;

    ///////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public AbstractActuator(Scenario scenario, jaxb.Actuator jaxb_actuator) throws OTMException {
        this.id = jaxb_actuator.getId();
        this.dt = jaxb_actuator.getDt();
        if(jaxb_actuator.getActuatorTarget()!=null){
            jaxb.ActuatorTarget e = jaxb_actuator.getActuatorTarget();
            Long id = e.getId()==null ? null : Long.parseLong(e.getId());

            ScenarioElementType type ;
            try {
                // this will throw an exception if the type is not a ScenarioElementType
                type = ScenarioElementType.valueOf(e.getType());

                // otherwise we can find the element and register

                // if it is a lanegroup, then the id is for the link, and lanes must be used
                if(type==ScenarioElementType.lanegroups)
                    this.target = OTMUtils.read_lanegroups(e.getLanegroups(),scenario);
                else
                    this.target = (InterfaceActuatorTarget) scenario.get_element(type,id);

                if(target!=null)
                    target.register_actuator(this);

            } catch (IllegalArgumentException illegalArgumentException) {
                // if exception is thrown, set target to null.
                // and resolve in higher level constructor
                this.target = null;
            }
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
    public final ScenarioElementType getSEType() {
        return ScenarioElementType.actuator;
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        poke(scenario.dispatcher,scenario.dispatcher.current_time);
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        if(target==null)
            errorLog.addWarning("Actuator has no target");
    }

    @Override
    public OTMErrorLog to_jaxb() {
        return null;
    }

    /////////////////////////////////////////////////////////////////////
    // Pokable
    /////////////////////////////////////////////////////////////////////

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
    // InterfaceEventWriter
    /////////////////////////////////////////////////////////////////////

//    @Override
//    public void set_event_output(AbstractOutputEvent e) throws OTMException {
//        if(event_output !=null)
//            throw new OTMException("multiple listeners for actuator.");
//        if(!(e instanceof OutputActuator))
//            throw new OTMException("Wrong type of listener");
//        event_output = (OutputActuator)e;
//    }

    /////////////////////////////////////////////////////////////////////
    // AbstractActuatorLanegroup
    /////////////////////////////////////////////////////////////////////

    protected Set<AbstractLaneGroup> read_lanegroups(Scenario scenario, Actuator jact) throws OTMException {
        if(jact.getActuatorTarget()==null || !jact.getActuatorTarget().getType().equalsIgnoreCase("lanegroups"))
            return null;
        jaxb.ActuatorTarget e = jact.getActuatorTarget();
        LaneGroupSet lgs = OTMUtils.read_lanegroups(e.getLanegroups(),scenario);
        return lgs.lgs;
    }

    /////////////////////////////////////////////////////////////////////
    // get
    /////////////////////////////////////////////////////////////////////

//    public Type getType() {
//        return type;
//    }

}
