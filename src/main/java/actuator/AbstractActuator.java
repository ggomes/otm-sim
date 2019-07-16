/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
*/

package actuator;

import control.AbstractController;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import dispatch.InterfacePokable;
import error.OTMErrorLog;
import error.OTMException;
import output.EventsActuator;
import runner.InterfaceScenarioElement;
import runner.Scenario;
import runner.ScenarioElementType;

public abstract class AbstractActuator implements InterfacePokable, InterfaceScenarioElement {

    public enum Type {
        signal,
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

    /////////////////////////////////////////////////////////////////////
    // construction and update
    /////////////////////////////////////////////////////////////////////

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

    public void validate(OTMErrorLog errorLog) {
        if(target==null)
            errorLog.addWarning("Actuator has no target");
    }

    abstract public void initialize(Scenario scenario) throws OTMException;

    public void register_with_dispatcher(Dispatcher dispatcher){
        dispatcher.register_event(new EventPoke(dispatcher,3,dispatcher.current_time,this));
    }

    /////////////////////////////////////////////////////////////////////
    // update
    /////////////////////////////////////////////////////////////////////

    abstract public void process_controller_command(Object command, Dispatcher dispatcher, float timestamp) throws OTMException;

    @Override
    public void poke(Dispatcher dispatcher, float timestamp) throws OTMException {

        // process the command
        if(myController!=null)
            process_controller_command(myController.get_command_for_actuator_id(id),dispatcher,timestamp);

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

    /////////////////////////////////////////////////////////////////////
    // scenario interactions
    /////////////////////////////////////////////////////////////////////

//    public void register_with_target() throws OTMException {
//        if(node==null)
//            return;
//        node.register_actuator(this);
//        for(_SignalPhaseNEMA sp : signal_phases.values())
//            sp.register_with_dispatcher(this);
//    }


    ////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public ScenarioElementType getScenarioElementType() {
        return ScenarioElementType.actuator;
    }

    public Type getType(){
        return type;
    }

}
