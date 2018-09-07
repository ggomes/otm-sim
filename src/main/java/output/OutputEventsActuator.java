/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package output;

import actuator.AbstractActuator;
import actuator.sigint.ActuatorSignal;
import actuator.sigint.SignalPhase;
import api.events.EventSignalPhase;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import runner.RunParameters;
import runner.Scenario;

public class OutputEventsActuator extends AbstractOutputEvent {

    public Long actuator_id;

    public OutputEventsActuator(Scenario scenario, String prefix, String output_folder, Long actuator_id) throws OTMException {
        super(scenario, prefix, output_folder);
        this.type = Type.actuator;

        if(actuator_id!=null)
            this.actuator_id = actuator_id;
        else
            throw new OTMException("Actuator id not defined");
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);

        AbstractActuator actuator = scenario.actuators.get(actuator_id);
        if(actuator_id==null || actuator==null)
            errorLog.addError("Bad actuator id in output request");
    }

    @Override
    public void register(RunParameters props, Dispatcher dispatcher) throws OTMException {

        AbstractActuator actuator = scenario.actuators.get(actuator_id);
        if(actuator!=null)
            actuator.set_event_listener(this);
        else   // register with all actuators
            for (AbstractActuator a : scenario.actuators.values())
                a.set_event_listener(this);

    }

    @Override
    public String get_output_file() {
        return super.get_output_file() + "_actuator_" + actuator_id + "_.txt";
    }

    public void plot(String filename) throws OTMException {

        AbstractActuator actuator = scenario.actuators.get(actuator_id);

        XYSeriesCollection dataset = null;

        if(actuator instanceof ActuatorSignal)
            dataset = plot_signal_actuator();

        if(dataset==null)
            throw new OTMException("Plotting not imlpemented for this type of actuator.");

        make_time_chart(dataset,"",filename);
    }

    //////////////////////////////////////////////////////
    // write
    //////////////////////////////////////////////////////

//    public void set_bulb_color(float timestamp,long controller_id,
//                               _NEMA.ID nema,
//                               BulbColor from_color,
//                               BulbColor to_color) throws OTMException {
//        this.write(timestamp,controller_id + "\t" + nema + "\t" + from_color + "\t" + to_color);
//    }

    //////////////////////////////////////////////////////
    // private
    //////////////////////////////////////////////////////

    private XYSeriesCollection plot_signal_actuator(){

        // get the actuator
        ActuatorSignal actuatorSignal = (ActuatorSignal)  scenario.actuators.get(actuator_id);

        // initialize the dataset. add series per phase
        XYSeriesCollection dataset = new XYSeriesCollection();
        for(SignalPhase signalPhase : actuatorSignal.signal_phases.values()){
            XYSeries series = new XYSeries(signalPhase.id);
            dataset.addSeries(series);
        }

        // go through events
        for(api.events.AbstractEvent absevent : this.events){
            EventSignalPhase e = (EventSignalPhase) absevent;

            XYSeries series =dataset.getSeries(e.signal_phase_id);

            int value = 0;
            switch (e.bulbcolor) {
                case RED:
                case DARK:
                    value = 0;
                    break;
                case YELLOW:
                    value = 1;
                    break;
                case GREEN:
                    value = 2;
                    break;
            }

            series.add(absevent.timestamp,value);
        }

        return dataset;
    }
}
