package output;

import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import runner.RunParameters;
import common.Scenario;
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
        return write_to_file ? super.get_output_file() + "_sensor_" + sensor_id + "_.txt" : null;
    }

    public void plot(String filename) throws OTMException {
        throw new OTMException("not implemented");
    }

}
