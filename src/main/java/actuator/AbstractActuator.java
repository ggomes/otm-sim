package actuator;

import core.*;
import control.AbstractController;
import control.command.InterfaceCommand;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import dispatch.Pokable;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Actuator;
import utils.OTMUtils;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractActuator implements Pokable, InterfaceScenarioElement {

    public enum Type {
        lg_restrict,
        lg_speed,
        signal,
        meter,
        stop,
        split,
        flowtolink
    }

    public long id;
    public abstract Type getType();
    public float dt;
    public boolean initialized;

    public AbstractController myController;
    public InterfaceActuatorTarget target;
    public Set<Long> commids; // not always used

    abstract public void process_controller_command(InterfaceCommand command, float timestamp) throws OTMException;

    ///////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public AbstractActuator(Scenario scenario, jaxb.Actuator jaxb_actuator) throws OTMException {
        this.id = jaxb_actuator.getId();
        this.dt = jaxb_actuator.getDt();
        this.initialized = false;
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
                    this.target = OTMUtils.read_lanegroups(e.getLanegroups(),scenario.network);
                else
                    this.target = (InterfaceActuatorTarget) scenario.get_element(type,id);

                if(e.getCommids()!=null) {
                    this.commids = new HashSet<>();
                    commids.addAll(OTMUtils.csv2longlist(e.getCommids()));
                }

                if(target!=null)
                    target.register_actuator(commids,this);

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

    public void initialize(Scenario scenario, float start_time) throws OTMException {

        if(initialized)
            return;

        if(dt>0f) {
            Dispatcher dispatcher = scenario.dispatcher;
            dispatcher.register_event(new EventPoke(dispatcher, 30, start_time, this));
        }
        initialized=true;
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

    protected Set<AbstractLaneGroup> read_lanegroups(Network network, Actuator jact) throws OTMException {
        if(jact.getActuatorTarget()==null || !jact.getActuatorTarget().getType().equalsIgnoreCase("lanegroups"))
            return null;
        jaxb.ActuatorTarget e = jact.getActuatorTarget();
        LaneGroupSet lgs = OTMUtils.read_lanegroups(e.getLanegroups(),network);
        return lgs.lgs;
    }

    /////////////////////////////////////////////////////////////////////
    // get
    /////////////////////////////////////////////////////////////////////

//    public Type getType() {
//        return type;
//    }

}
