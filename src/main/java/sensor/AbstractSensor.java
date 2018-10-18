/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package sensor;

import dispatch.Dispatcher;
import dispatch.EventPoke;
import dispatch.InterfacePokable;
import error.OTMErrorLog;
import error.OTMException;
import output.EventsSensor;
import runner.InterfaceScenarioElement;
import runner.RunParameters;
import runner.Scenario;
import runner.ScenarioElementType;

public abstract class AbstractSensor implements InterfacePokable, InterfaceScenarioElement {

    public enum Type {
        fixed,
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

    public void validate(OTMErrorLog errorLog){

    }

    public void initialize(Scenario scenario, RunParameters runParams) throws OTMException{

    }

    public void register_initial_events(Dispatcher dispatcher){
        dispatcher.register_event(new EventPoke(dispatcher,4,dispatcher.current_time,this));
    }

    /////////////////////////////////////////////////////////////////////
    // update
    /////////////////////////////////////////////////////////////////////

    abstract public void take_measurement(Dispatcher dispatcher, float timestamp);

    @Override
    public void poke(Dispatcher dispatcher, float timestamp) throws OTMException {
        take_measurement(dispatcher,timestamp);

        // wake up in dt, if dt is defined
        if(dt>0)
            dispatcher.register_event(new EventPoke(dispatcher,4,timestamp+dt,this));
    }

    /////////////////////////////////////////////////////////////////////
    // listeners
    /////////////////////////////////////////////////////////////////////

    public void set_event_listener(EventsSensor e) throws OTMException {
        if(event_listener!=null)
            throw new OTMException("multiple listeners for commodity");
        event_listener = e;
    }

    ////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public ScenarioElementType getScenarioElementType() {
        return ScenarioElementType.sensor;
    }

}
