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
import output.OutputEventsSensor;
import runner.InterfaceScenarioElement;
import runner.RunParameters;
import runner.Scenario;
import runner.ScenarioElementType;

public abstract class AbstractSensor implements InterfacePokable, InterfaceScenarioElement {

    public enum Type {
        loop,
        plugin
    }

    public long id;
    public Type type;
    public float dt;

    public Object target;

    public OutputEventsSensor event_listener;

    /////////////////////////////////////////////////////////////////////
    // construction
    /////////////////////////////////////////////////////////////////////

    public AbstractSensor(Scenario scenario, jaxb.Sensor jaxb_sensor) {
        this.id = jaxb_sensor.getId();
        this.type = Type.valueOf(jaxb_sensor.getType());
        this.dt = jaxb_sensor.getDt();
    }

    abstract public void validate(OTMErrorLog errorLog);

    abstract public void initialize(Scenario scenario, RunParameters runParams) throws OTMException;

    /////////////////////////////////////////////////////////////////////
    // update
    /////////////////////////////////////////////////////////////////////

    abstract public void take_measurement(Dispatcher dispatcher, float timestamp);

    @Override
    public void poke(Dispatcher dispatcher, float timestamp) throws OTMException {
        take_measurement(dispatcher,timestamp);

        // wake up in dt, if dt is defined
        if(dt>0)
            dispatcher.register_event(new EventPoke(dispatcher,1,timestamp+dt,this));
    }

    /////////////////////////////////////////////////////////////////////
    // listeners
    /////////////////////////////////////////////////////////////////////

    public void set_event_listener(OutputEventsSensor e) throws OTMException {
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
