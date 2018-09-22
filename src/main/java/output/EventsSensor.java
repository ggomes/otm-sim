/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package output;

import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import runner.RunParameters;
import runner.Scenario;
import sensor.AbstractSensor;

public class EventsSensor extends AbstractOutputEvent  {

    public Long sensor_id;

    public EventsSensor(Scenario scenario, String prefix, String output_folder, Long sensor_id) throws OTMException {
        super(scenario, prefix, output_folder);
        this.type = Type.sensor;

        if(sensor_id!=null)
            this.sensor_id = sensor_id;
        else
            throw new OTMException("Sensor id not defined");
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);

        AbstractSensor sensor = scenario.sensors.get(sensor_id);
        if(sensor_id==null || sensor==null)
            errorLog.addError("Bad sensor id in output request");
    }

    @Override
    public void register(RunParameters props, Dispatcher dispatcher) throws OTMException {

        AbstractSensor sensor = scenario.sensors.get(sensor_id);
        if(sensor!=null)
            sensor.set_event_listener(this);
        else   // register with all actuators
            for (AbstractSensor s : scenario.sensors.values())
                s.set_event_listener(this);

    }

    @Override
    public String get_output_file() {
        return super.get_output_file() + "_sensor_" + sensor_id + "_.txt";
    }

    public void plot(String filename) throws OTMException {
        throw new OTMException("not implemented");
    }

}
